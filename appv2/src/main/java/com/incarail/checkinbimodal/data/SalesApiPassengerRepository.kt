package com.incarail.checkinbimodal.data

import com.incarail.checkinbimodal.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Implementación real de [PassengerRepository]: lee la lista de pasajeros con servicio bimodal
 * desde la hoja de Google Sheets conectada a BigQuery, a través del pequeño Web App de Apps
 * Script que la expone como JSON (ver /integracion en la raíz del proyecto e
 * INTEGRACION_DATOS.md para el detalle de cómo se arma esa cadena).
 *
 * Esa hoja/BigQuery es la ÚNICA fuente de verdad de "quién compró y debería abordar". El estado
 * de abordaje (BOARDED/NO_SHOW), la ficha del pasajero (incidencias/condición médica) y la
 * tripulación/proveedores que el colaborador registra en el teléfono viven solo en esta
 * instancia mientras dura el turno — todavía no existe un backend de escritura que los reciba
 * (ver README, sección pendiente sobre JESAB/GOP); [closeManifest] es el punto exacto donde,
 * en esa fase futura, se enviaría ese resumen.
 */
class SalesApiPassengerRepository : PassengerRepository {

    private val baseUrl = BuildConfig.SALES_API_BASE_URL
    private val token = BuildConfig.SALES_API_TOKEN

    // Cache en memoria: se llena en la primera consulta del turno y luego se muta localmente
    // según lo que el colaborador va marcando. Un pull-to-refresh o reabrir la app vuelve a
    // consultar el origen (útil porque la hoja se refresca sola cada ~15-20 min).
    @Volatile private var frequenciesCache: MutableList<Frequency>? = null
    private val passengersCache: MutableMap<String, MutableList<Passenger>> = mutableMapOf()
    private val crewCache: MutableMap<String, MutableList<CrewMember>> = mutableMapOf()

    override suspend fun getFrequencies(): List<Frequency> {
        ensureLoaded()
        return frequenciesCache.orEmpty().toList()
    }

    override suspend fun getPassengers(frequencyId: String): List<Passenger> {
        ensureLoaded()
        return passengersCache[frequencyId]?.toList().orEmpty()
    }

    override suspend fun updatePassengerStatus(frequencyId: String, passengerId: String, status: BoardingStatus) {
        passengersCache[frequencyId]?.replaceAll {
            if (it.id == passengerId) {
                val newStatus = if (it.status == status) BoardingStatus.PENDING else status
                it.copy(
                    status = newStatus,
                    registeredAt = if (newStatus == BoardingStatus.PENDING) null else System.currentTimeMillis(),
                    registeredBy = if (newStatus == BoardingStatus.PENDING) null else "Manual",
                )
            } else it
        }
    }

    override suspend fun updatePassengerDetails(frequencyId: String, passenger: Passenger) {
        passengersCache[frequencyId]?.replaceAll { if (it.id == passenger.id) passenger else it }
    }

    override suspend fun addWalkIn(frequencyId: String, name: String, document: String, reason: String): Passenger {
        val newPax = Passenger(
            id = "walkin-${System.currentTimeMillis()}",
            name = name,
            document = document.ifBlank { "Sin documento" },
            type = "Adicional",
            status = BoardingStatus.BOARDED,
            isWalkIn = true,
            registeredAt = System.currentTimeMillis(),
            registeredBy = "Manual",
        )
        passengersCache.getOrPut(frequencyId) { mutableListOf() }.add(newPax)
        return newPax
    }

    override suspend fun closeManifest(frequencyId: String): BoardingSummary {
        val pax = passengersCache[frequencyId].orEmpty()
        frequenciesCache?.replaceAll { if (it.id == frequencyId) it.copy(status = FrequencyStatus.CLOSED) else it }
        return BoardingSummary(
            frequencyId = frequencyId,
            boarded = pax.count { it.status == BoardingStatus.BOARDED },
            noShow = pax.count { it.status == BoardingStatus.NO_SHOW },
            additional = pax.count { it.isWalkIn },
            closedAtEpochMillis = System.currentTimeMillis(),
        )
        // TODO (siguiente fase, fuera de este alcance): enviar este resumen a un backend real
        // (JESAB/GOP) en vez de quedarse solo en memoria.
    }

    override suspend fun registerBoardingByCode(frequencyId: String, code: String, registeredBy: String): BoardingRegistrationResult {
        val normalized = code.trim()
        passengersCache[frequencyId]?.find { it.document.endsWith(normalized) || it.id == normalized }?.let { pax ->
            if (pax.registeredAt != null) return BoardingRegistrationResult.AlreadyRegistered(pax)
            val updated = pax.copy(status = BoardingStatus.BOARDED, registeredAt = System.currentTimeMillis(), registeredBy = registeredBy)
            passengersCache[frequencyId]?.replaceAll { if (it.id == pax.id) updated else it }
            return BoardingRegistrationResult.Success(updated)
        }
        for ((otherFreqId, list) in passengersCache) {
            if (otherFreqId == frequencyId) continue
            val found = list.find { it.document.endsWith(normalized) || it.id == normalized }
            if (found != null) {
                val actualFreq = frequenciesCache.orEmpty().find { it.id == otherFreqId } ?: continue
                return BoardingRegistrationResult.WrongFrequency(found, actualFreq)
            }
        }
        return BoardingRegistrationResult.NotFound
    }

    override suspend fun getCrew(frequencyId: String): List<CrewMember> = crewCache[frequencyId]?.toList().orEmpty()

    override suspend fun lookupCrewByCode(code: String): CrewRegistrationResult {
        // El origen de datos actual (hoja de Sheets de ventas) no trae un directorio de
        // tripulación/proveedores — siempre cae al formulario manual (ver R. Tripulación).
        return CrewRegistrationResult.NotFound
    }

    override suspend fun addCrewMember(frequencyId: String, role: PersonRole, name: String, document: String, phone: String, nationality: String): CrewMember {
        val member = CrewMember(id = "crew-${System.currentTimeMillis()}", role = role, name = name, document = document, phone = phone, nationality = nationality)
        crewCache.getOrPut(frequencyId) { mutableListOf() }.add(member)
        return member
    }

    /** Fuerza una recarga desde el origen en la próxima llamada (útil para un pull-to-refresh futuro). */
    fun invalidate() {
        frequenciesCache = null
        passengersCache.clear()
    }

    private suspend fun ensureLoaded() {
        if (frequenciesCache != null) return
        withContext(Dispatchers.IO) {
            val json = fetchJson()
            val error = json.optString("error", "")
            check(error.isEmpty()) { "El origen de datos respondió con un error: $error" }

            val freqArray = json.getJSONArray("frequencies")
            val freqs = mutableListOf<Frequency>()

            for (i in 0 until freqArray.length()) {
                val f = freqArray.getJSONObject(i)
                val freqId = f.getString("id")
                val passengersJson = f.getJSONArray("passengers")
                val passengers = mutableListOf<Passenger>()
                for (j in 0 until passengersJson.length()) {
                    val p = passengersJson.getJSONObject(j)
                    passengers.add(
                        Passenger(
                            id = p.getString("id"),
                            name = p.getString("name"),
                            document = p.optString("document", ""),
                            type = p.optString("type", "Directo"),
                        )
                    )
                }
                passengersCache[freqId] = passengers
                // El pipe actual (BigQuery de ventas bimodal) solo trae buses; si más adelante
                // también se conecta tren, el JSON puede incluir "mode":"TREN" por frecuencia.
                val mode = if (f.optString("mode", "BUS") == "TREN") ServiceMode.TREN else ServiceMode.BUS
                freqs.add(
                    Frequency(
                        id = freqId,
                        mode = mode,
                        time = f.getString("time"),
                        route = f.getString("route"),
                        unit = f.optString("unit", ""),
                        provider = f.optString("provider", null.toString()).takeIf { it.isNotBlank() && it != "null" },
                        conductor = f.optString("conductor", null.toString()).takeIf { it.isNotBlank() && it != "null" },
                        expectedPax = passengers.size,
                        status = FrequencyStatus.PENDING,
                    )
                )
            }
            frequenciesCache = freqs
        }
    }

    private fun fetchJson(): JSONObject {
        check(baseUrl.isNotBlank()) {
            "SALES_API_BASE_URL no está configurado. Revisa local.properties (ver INTEGRACION_DATOS.md)."
        }
        val urlStr = "$baseUrl?token=${URLEncoder.encode(token, "UTF-8")}"
        val connection = URL(urlStr).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream.bufferedReader().use { it.readText() }
            return JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }
}
