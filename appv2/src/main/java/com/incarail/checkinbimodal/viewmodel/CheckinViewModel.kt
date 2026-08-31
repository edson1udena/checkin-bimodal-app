package com.incarail.checkinbimodal.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.incarail.checkinbimodal.data.BoardingRegistrationResult
import com.incarail.checkinbimodal.data.BoardingStatus
import com.incarail.checkinbimodal.data.BoardingSummary
import com.incarail.checkinbimodal.data.CrewMember
import com.incarail.checkinbimodal.data.CrewRegistrationResult
import com.incarail.checkinbimodal.data.Frequency
import com.incarail.checkinbimodal.data.MockPassengerRepository
import com.incarail.checkinbimodal.data.Passenger
import com.incarail.checkinbimodal.data.PassengerRepository
import com.incarail.checkinbimodal.data.PersonRole
import com.incarail.checkinbimodal.data.ServiceMode
import kotlinx.coroutines.launch

/**
 * ViewModel único para el flujo de check-in. Recibe el repositorio por constructor:
 * hoy se le inyecta [MockPassengerRepository] o [com.incarail.checkinbimodal.data.SalesApiPassengerRepository]
 * según la configuración (ver MainActivity); el resto de la app no conoce esa diferencia.
 */
class CheckinViewModel(
    private val repository: PassengerRepository = MockPassengerRepository(),
) : ViewModel() {

    val userName = mutableStateOf("")
    val modeFilter = mutableStateOf(ServiceMode.BUS)

    val frequencies = mutableStateOf<List<Frequency>>(emptyList())
    val passengers = mutableStateOf<List<Passenger>>(emptyList())
    val selectedFrequency = mutableStateOf<Frequency?>(null)
    val selectedPassenger = mutableStateOf<Passenger?>(null)
    val lastSummary = mutableStateOf<BoardingSummary?>(null)
    val searchQuery = mutableStateOf("")

    val crew = mutableStateOf<List<CrewMember>>(emptyList())

    fun setUserName(name: String) {
        userName.value = name.ifBlank { "Colaborador SAB" }
    }

    fun loadFrequencies() {
        viewModelScope.launch {
            frequencies.value = repository.getFrequencies()
        }
    }

    fun frequenciesForMode(mode: ServiceMode): List<Frequency> = frequencies.value.filter { it.mode == mode }

    fun selectFrequency(freq: Frequency) {
        selectedFrequency.value = freq
        searchQuery.value = ""
        viewModelScope.launch {
            passengers.value = repository.getPassengers(freq.id)
            crew.value = repository.getCrew(freq.id)
        }
    }

    fun selectPassenger(pax: Passenger) {
        selectedPassenger.value = pax
    }

    fun setStatus(passengerId: String, status: BoardingStatus) {
        val freqId = selectedFrequency.value?.id ?: return
        viewModelScope.launch {
            repository.updatePassengerStatus(freqId, passengerId, status)
            passengers.value = repository.getPassengers(freqId)
        }
    }

    fun savePassengerDetails(passenger: Passenger) {
        val freqId = selectedFrequency.value?.id ?: return
        viewModelScope.launch {
            repository.updatePassengerDetails(freqId, passenger)
            passengers.value = repository.getPassengers(freqId)
            selectedPassenger.value = passenger
        }
    }

    /** Registro por QR/código escaneado o digitado. Ver validaciones (vagón/frecuencia incorrecta, duplicidad). */
    suspend fun registerBoardingByCode(code: String): BoardingRegistrationResult {
        val freqId = selectedFrequency.value?.id ?: return BoardingRegistrationResult.NotFound
        val result = repository.registerBoardingByCode(freqId, code, userName.value.ifBlank { "Colaborador SAB" })
        if (result is BoardingRegistrationResult.Success) {
            passengers.value = repository.getPassengers(freqId)
        }
        return result
    }

    fun addWalkIn(name: String, document: String, reason: String) {
        val freqId = selectedFrequency.value?.id ?: return
        viewModelScope.launch {
            repository.addWalkIn(freqId, name, document, reason)
            passengers.value = repository.getPassengers(freqId)
        }
    }

    fun closeManifest() {
        val freqId = selectedFrequency.value?.id ?: return
        viewModelScope.launch {
            lastSummary.value = repository.closeManifest(freqId)
            frequencies.value = repository.getFrequencies()
        }
    }

    suspend fun lookupCrewByCode(code: String): CrewRegistrationResult = repository.lookupCrewByCode(code)

    fun addCrewMember(role: PersonRole, name: String, document: String, phone: String, nationality: String) {
        val freqId = selectedFrequency.value?.id ?: return
        viewModelScope.launch {
            repository.addCrewMember(freqId, role, name, document, phone, nationality)
            crew.value = repository.getCrew(freqId)
        }
    }

    fun filteredPassengers(): List<Passenger> {
        val q = searchQuery.value.trim().lowercase()
        if (q.isEmpty()) return passengers.value
        return passengers.value.filter {
            it.name.lowercase().contains(q) || it.document.lowercase().contains(q)
        }
    }
}
