package com.incarail.checkinbimodal.data

/**
 * Resultado de intentar registrar el abordaje de un pasajero a partir de un código escaneado
 * (QR) o digitado (boleto/documento). Ver brief "Aplicativo Gestión de Abordaje Tren & Bus",
 * validaciones 1.1/3.1 (vagón/frecuencia incorrecta, control de duplicidad).
 */
sealed class BoardingRegistrationResult {
    data class Success(val passenger: Passenger) : BoardingRegistrationResult()
    data class AlreadyRegistered(val passenger: Passenger) : BoardingRegistrationResult()
    data class WrongFrequency(val passenger: Passenger, val actualFrequency: Frequency) : BoardingRegistrationResult()
    object NotFound : BoardingRegistrationResult()
}

/** Resultado de intentar registrar tripulación/proveedor a partir de un código escaneado. */
sealed class CrewRegistrationResult {
    data class Found(val crew: CrewMember) : CrewRegistrationResult()
    object NotFound : CrewRegistrationResult()
}

/**
 * Contrato del repositorio de datos. La UI y el ViewModel solo conocen esta interfaz.
 *
 * FASE DE INTEGRACIÓN: [SalesApiPassengerRepository] implementa esta misma interfaz leyendo
 * de la hoja de Google Sheets conectada a BigQuery (ver INTEGRACION_DATOS.md). Mientras esa
 * integración no esté configurada, la app funciona con [MockPassengerRepository].
 *
 * `closeManifest` es también el punto donde, en producción, se enviaría el resumen al
 * backend intermedio para que JESAB/GOP lo vean (hoy solo lo deja registrado en memoria/local).
 */
interface PassengerRepository {
    suspend fun getFrequencies(): List<Frequency>
    suspend fun getPassengers(frequencyId: String): List<Passenger>
    suspend fun updatePassengerStatus(frequencyId: String, passengerId: String, status: BoardingStatus)
    suspend fun updatePassengerDetails(frequencyId: String, passenger: Passenger)
    suspend fun addWalkIn(frequencyId: String, name: String, document: String, reason: String): Passenger
    suspend fun closeManifest(frequencyId: String): BoardingSummary

    /** Registro por QR/código: busca el pasajero en TODAS las frecuencias, no solo en [frequencyId]. */
    suspend fun registerBoardingByCode(frequencyId: String, code: String, registeredBy: String): BoardingRegistrationResult

    suspend fun getCrew(frequencyId: String): List<CrewMember>
    suspend fun lookupCrewByCode(code: String): CrewRegistrationResult
    suspend fun addCrewMember(frequencyId: String, role: PersonRole, name: String, document: String, phone: String, nationality: String): CrewMember
}

/**
 * Implementación de ejemplo usada para el prototipo y para desarrollo sin conexión al
 * sistema de ventas real. Los datos aquí son ficticios (no corresponden a pasajeros reales).
 * Incluye ejemplos de tren y de bus para poder probar ambos modos.
 */
class MockPassengerRepository : PassengerRepository {

    private val frequencies = mutableListOf(
        Frequency("f1", ServiceMode.BUS, "06:10", "Cusco → Ollantaytambo", "Sprinter 04", expectedPax = 11, status = FrequencyStatus.PENDING),
        Frequency("f2", ServiceMode.BUS, "07:40", "Cusco → Ollantaytambo", "Sprinter 02", expectedPax = 9, status = FrequencyStatus.IN_PROGRESS),
        Frequency("f3", ServiceMode.BUS, "09:15", "Ollantaytambo → Cusco", "Sprinter 06", expectedPax = 13, status = FrequencyStatus.CLOSED),
        Frequency("t1", ServiceMode.TREN, "06:10", "Ollantaytambo → Machu Picchu", "975", provider = null, conductor = null, expectedPax = 3, status = FrequencyStatus.PENDING),
        Frequency("t2", ServiceMode.TREN, "07:40", "Cusco → Ollantaytambo", "974", expectedPax = 2, status = FrequencyStatus.PENDING),
    )

    private val passengersByFrequency: MutableMap<String, MutableList<Passenger>> = mutableMapOf(
        "f1" to mutableListOf(
            Passenger("p1", "Rosa Delgado Vega", "DNI 45812093", "Directo", phone = "999888771", email = "rosa.delgado@example.com"),
            Passenger("p2", "Marco Antonio Ríos", "DNI 09931244", "Agencia"),
            Passenger("p3", "Helena Fischer", "PAS X3391882", "Agencia", nationality = "Alemania"),
            Passenger("p4", "Jonas Weber", "PAS X9012341", "Agencia", nationality = "Alemania"),
            Passenger("p5", "Lucía Farfán", "DNI 71029384", "Directo"),
            Passenger("p6", "Diego Salazar", "DNI 40233110", "Directo"),
            Passenger("p7", "Mei Tanaka", "PAS TK102938", "Agencia", nationality = "Japón"),
            Passenger("p8", "Andrés Chávez", "DNI 08812093", "Directo", hasMedicalCondition = true, medicalConditionDescription = "Hipertensión"),
            Passenger("p9", "Carla Ibáñez", "DNI 71982031", "Agencia"),
            Passenger("p10", "Paul Novak", "PAS CZ88213", "Directo", nationality = "República Checa"),
            Passenger("p11", "Sofía Ramos", "DNI 60293114", "Directo", hasSpecialRequirement = true, specialRequirementDescription = "Movilidad reducida"),
        ),
        "f2" to mutableListOf(
            Passenger("q1", "Renata Costa", "PAS BR90213", "Agencia", nationality = "Brasil"),
            Passenger("q2", "Iker Gonzáles", "DNI 45213098", "Directo"),
            Passenger("q3", "Emma Clarke", "PAS UK55213", "Agencia", nationality = "Reino Unido"),
            Passenger("q4", "Julio Prado", "DNI 08213097", "Directo"),
        ),
        "f3" to mutableListOf(
            Passenger("r1", "Grace Miller", "PAS US44213", "Agencia", nationality = "Estados Unidos"),
            Passenger("r2", "Fernando Quiroz", "DNI 08213456", "Directo"),
        ),
        "t1" to mutableListOf(
            Passenger("s1", "Johan Huaman Quispe", "12345789", "Directo", seat = "20", product = "The Voyager", phone = "999888777"),
            Passenger("s2", "Joaquín Rodríguez Curioso", "78956211", "Directo", seat = "7", product = "Imperdibles de Lujo Machu Picchu 3 días", hasMedicalCondition = true, medicalConditionDescription = "Diabetes"),
            Passenger("s3", "Jhon Gonzales Huaman", "987654321", "Directo", seat = "8"),
        ),
        "t2" to mutableListOf(
            Passenger("u1", "Sandra Carola Abad", "1548654562", "Directo", seat = "12"),
            Passenger("u2", "Tino Reyna Monteverde", "987654321", "Directo", seat = "13"),
        ),
    )

    private val crewByFrequency: MutableMap<String, MutableList<CrewMember>> = mutableMapOf()

    // Directorio simulado de personal ya registrado en el sistema (lo que el brief asume que
    // existe): permite que escanear su QR "encuentre" sus datos automáticamente. Cualquier otro
    // código cae al formulario manual (ver R. Tripulación).
    private val crewDirectory: Map<String, CrewMember> = mapOf(
        "12345789" to CrewMember("crew-1", PersonRole.TRIPULACION, "Johan Huaman Quispe", "12345789", "999888777", "Perú"),
        "13456789" to CrewMember("crew-2", PersonRole.PROVEEDOR, "Fernando García", "13456789", "999888777", "Perú"),
    )

    override suspend fun getFrequencies(): List<Frequency> = frequencies.toList()

    override suspend fun getPassengers(frequencyId: String): List<Passenger> =
        passengersByFrequency[frequencyId]?.toList().orEmpty()

    override suspend fun updatePassengerStatus(frequencyId: String, passengerId: String, status: BoardingStatus) {
        passengersByFrequency[frequencyId]?.replaceAll {
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
        passengersByFrequency[frequencyId]?.replaceAll { if (it.id == passenger.id) passenger else it }
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
        passengersByFrequency.getOrPut(frequencyId) { mutableListOf() }.add(newPax)
        return newPax
    }

    override suspend fun closeManifest(frequencyId: String): BoardingSummary {
        val pax = passengersByFrequency[frequencyId].orEmpty()
        frequencies.replaceAll { if (it.id == frequencyId) it.copy(status = FrequencyStatus.CLOSED) else it }
        return BoardingSummary(
            frequencyId = frequencyId,
            boarded = pax.count { it.status == BoardingStatus.BOARDED },
            noShow = pax.count { it.status == BoardingStatus.NO_SHOW },
            additional = pax.count { it.isWalkIn },
            closedAtEpochMillis = System.currentTimeMillis(),
        )
    }

    override suspend fun registerBoardingByCode(frequencyId: String, code: String, registeredBy: String): BoardingRegistrationResult {
        val normalized = code.trim()
        // Busca primero en la frecuencia configurada (caso normal).
        passengersByFrequency[frequencyId]?.find { it.document.endsWith(normalized) || it.id == normalized }?.let { pax ->
            if (pax.registeredAt != null) return BoardingRegistrationResult.AlreadyRegistered(pax)
            val updated = pax.copy(status = BoardingStatus.BOARDED, registeredAt = System.currentTimeMillis(), registeredBy = registeredBy)
            passengersByFrequency[frequencyId]?.replaceAll { if (it.id == pax.id) updated else it }
            return BoardingRegistrationResult.Success(updated)
        }
        // No está en esta frecuencia: busca en todas las demás para poder avisar "no pertenece aquí".
        for ((otherFreqId, list) in passengersByFrequency) {
            if (otherFreqId == frequencyId) continue
            val found = list.find { it.document.endsWith(normalized) || it.id == normalized }
            if (found != null) {
                val actualFreq = frequencies.find { it.id == otherFreqId } ?: continue
                return BoardingRegistrationResult.WrongFrequency(found, actualFreq)
            }
        }
        return BoardingRegistrationResult.NotFound
    }

    override suspend fun getCrew(frequencyId: String): List<CrewMember> = crewByFrequency[frequencyId]?.toList().orEmpty()

    override suspend fun lookupCrewByCode(code: String): CrewRegistrationResult {
        val found = crewDirectory[code.trim()]
        return if (found != null) CrewRegistrationResult.Found(found) else CrewRegistrationResult.NotFound
    }

    override suspend fun addCrewMember(frequencyId: String, role: PersonRole, name: String, document: String, phone: String, nationality: String): CrewMember {
        val member = CrewMember(id = "crew-${System.currentTimeMillis()}", role = role, name = name, document = document, phone = phone, nationality = nationality)
        crewByFrequency.getOrPut(frequencyId) { mutableListOf() }.add(member)
        return member
    }
}
