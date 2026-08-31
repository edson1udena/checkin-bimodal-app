package com.incarail.checkinbimodal.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.incarail.checkinbimodal.data.BoardingStatus
import com.incarail.checkinbimodal.data.BoardingSummary
import com.incarail.checkinbimodal.data.Frequency
import com.incarail.checkinbimodal.data.MockPassengerRepository
import com.incarail.checkinbimodal.data.Passenger
import com.incarail.checkinbimodal.data.PassengerRepository
import kotlinx.coroutines.launch

/**
 * ViewModel único para el flujo de check-in. Recibe el repositorio por constructor:
 * hoy se le inyecta [MockPassengerRepository]; el día que exista la integración real basta
 * con pasarle la nueva implementación (p.ej. `SalesApiPassengerRepository`) sin tocar la UI.
 */
class CheckinViewModel(
    private val repository: PassengerRepository = MockPassengerRepository(),
) : ViewModel() {

    val frequencies = mutableStateOf<List<Frequency>>(emptyList())
    val passengers = mutableStateOf<List<Passenger>>(emptyList())
    val selectedFrequency = mutableStateOf<Frequency?>(null)
    val lastSummary = mutableStateOf<BoardingSummary?>(null)
    val searchQuery = mutableStateOf("")

    fun loadFrequencies() {
        viewModelScope.launch {
            frequencies.value = repository.getFrequencies()
        }
    }

    fun selectFrequency(freq: Frequency) {
        selectedFrequency.value = freq
        searchQuery.value = ""
        viewModelScope.launch {
            passengers.value = repository.getPassengers(freq.id)
        }
    }

    fun setStatus(passengerId: String, status: BoardingStatus) {
        val freqId = selectedFrequency.value?.id ?: return
        viewModelScope.launch {
            repository.updatePassengerStatus(freqId, passengerId, status)
            passengers.value = repository.getPassengers(freqId)
        }
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

    fun filteredPassengers(): List<Passenger> {
        val q = searchQuery.value.trim().lowercase()
        if (q.isEmpty()) return passengers.value
        return passengers.value.filter {
            it.name.lowercase().contains(q) || it.document.lowercase().contains(q)
        }
    }
}
