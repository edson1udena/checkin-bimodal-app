package com.incarail.checkinbimodal.data

/**
 * Contrato del repositorio de datos. La UI y el ViewModel solo conocen esta interfaz.
 *
 * FASE DE INTEGRACIÓN (pendiente, a propósito): crear `SalesApiPassengerRepository`
 * que implemente esta misma interfaz llamando al endpoint real del sistema de ventas
 * de Inca Rail (autenticación, URL base, y el mapeo de su modelo de reserva a [Passenger]
 * se definen ahí). Mientras esa clase no exista, la app funciona con [MockPassengerRepository].
 *
 * `closeManifest` es también el punto donde, en producción, se enviaría el resumen al
 * backend intermedio para que JESAB/GOP lo vean (hoy solo lo deja registrado en memoria/local).
 */
interface PassengerRepository {
    suspend fun getFrequencies(): List<Frequency>
    suspend fun getPassengers(frequencyId: String): List<Passenger>
    suspend fun updatePassengerStatus(frequencyId: String, passengerId: String, status: BoardingStatus)
    suspend fun addWalkIn(frequencyId: String, name: String, document: String, reason: String): Passenger
    suspend fun closeManifest(frequencyId: String): BoardingSummary
}

/**
 * Implementación de ejemplo usada para el prototipo y para desarrollo sin conexión al
 * sistema de ventas real. Los datos aquí son ficticios (no corresponden a pasajeros reales).
 */
class MockPassengerRepository : PassengerRepository {

    private val frequencies = mutableListOf(
        Frequency("f1", "06:10", "Cusco → Ollantaytambo", "Sprinter 04", 11, FrequencyStatus.PENDING),
        Frequency("f2", "07:40", "Cusco → Ollantaytambo", "Sprinter 02", 9, FrequencyStatus.IN_PROGRESS),
        Frequency("f3", "09:15", "Ollantaytambo → Cusco", "Sprinter 06", 13, FrequencyStatus.CLOSED),
    )

    private val passengersByFrequency: MutableMap<String, MutableList<Passenger>> = mutableMapOf(
        "f1" to mutableListOf(
            Passenger("p1", "Rosa Delgado Vega", "DNI 45812093", "Directo"),
            Passenger("p2", "Marco Antonio Ríos", "DNI 09931244", "Agencia"),
            Passenger("p3", "Helena Fischer", "PAS X3391882", "Agencia"),
            Passenger("p4", "Jonas Weber", "PAS X9012341", "Agencia"),
            Passenger("p5", "Lucía Farfán", "DNI 71029384", "Directo"),
            Passenger("p6", "Diego Salazar", "DNI 40233110", "Directo"),
            Passenger("p7", "Mei Tanaka", "PAS TK102938", "Agencia"),
            Passenger("p8", "Andrés Chávez", "DNI 08812093", "Directo"),
            Passenger("p9", "Carla Ibáñez", "DNI 71982031", "Agencia"),
            Passenger("p10", "Paul Novak", "PAS CZ88213", "Directo"),
            Passenger("p11", "Sofía Ramos", "DNI 60293114", "Directo"),
        ),
        "f2" to mutableListOf(
            Passenger("q1", "Renata Costa", "PAS BR90213", "Agencia"),
            Passenger("q2", "Iker Gonzáles", "DNI 45213098", "Directo"),
            Passenger("q3", "Emma Clarke", "PAS UK55213", "Agencia"),
            Passenger("q4", "Julio Prado", "DNI 08213097", "Directo"),
        ),
        "f3" to mutableListOf(
            Passenger("r1", "Grace Miller", "PAS US44213", "Agencia"),
            Passenger("r2", "Fernando Quiroz", "DNI 08213456", "Directo"),
        ),
    )

    override suspend fun getFrequencies(): List<Frequency> = frequencies.toList()

    override suspend fun getPassengers(frequencyId: String): List<Passenger> =
        passengersByFrequency[frequencyId]?.toList().orEmpty()

    override suspend fun updatePassengerStatus(frequencyId: String, passengerId: String, status: BoardingStatus) {
        passengersByFrequency[frequencyId]
            ?.replaceAll { if (it.id == passengerId) it.copy(status = if (it.status == status) BoardingStatus.PENDING else status) else it }
    }

    override suspend fun addWalkIn(frequencyId: String, name: String, document: String, reason: String): Passenger {
        val newPax = Passenger(
            id = "walkin-${System.currentTimeMillis()}",
            name = name,
            document = document.ifBlank { "Sin documento" },
            type = "Adicional",
            status = BoardingStatus.BOARDED,
            isWalkIn = true,
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
}
