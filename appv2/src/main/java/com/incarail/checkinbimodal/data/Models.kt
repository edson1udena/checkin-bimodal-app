package com.incarail.checkinbimodal.data

/**
 * Modelos de dominio. Estos mismos modelos se llenarán, en la fase de integración,
 * con la respuesta real de la API de ventas de Inca Rail en lugar de los datos de ejemplo
 * de [MockPassengerRepository].
 *
 * Ampliado para cubrir tren + bus (ver brief "Aplicativo Gestión de Abordaje Tren & Bus"),
 * de forma incremental sobre lo ya construido para bimodal: el modo emergencia, la
 * generación/impresión de manifiestos PDF, el escaneo OCR de pasaporte/DNI, la consola de
 * administración y la escritura en tiempo real hacia las bases de datos de Inca Rail
 * quedan fuera de este incremento (ver README para el detalle de por qué).
 */

enum class BoardingStatus { PENDING, BOARDED, NO_SHOW }

enum class FrequencyStatus { PENDING, IN_PROGRESS, CLOSED }

enum class ServiceMode { TREN, BUS }

enum class PersonRole { TRIPULACION, PROVEEDOR }

data class Frequency(
    val id: String,
    val mode: ServiceMode,
    val time: String,
    val route: String,
    val unit: String, // vagón (tren) o placa (bus)
    val provider: String? = null, // solo bus
    val conductor: String? = null, // solo bus
    val expectedPax: Int,
    var status: FrequencyStatus,
)

data class Passenger(
    val id: String,
    val name: String,
    val document: String,
    val type: String, // "Directo" | "Agencia" | "Adicional"
    var status: BoardingStatus = BoardingStatus.PENDING,
    val isWalkIn: Boolean = false,
    // Ficha del pasajero (brief 1.3/3.3)
    val phone: String = "",
    val email: String = "",
    val nationality: String = "Perú",
    val seat: String = "",
    val product: String = "",
    val hasMedicalCondition: Boolean = false,
    val medicalConditionDescription: String = "",
    val hasSpecialRequirement: Boolean = false,
    val specialRequirementDescription: String = "",
    val incidentType: String = "",
    val incidentDescription: String = "",
    // Control de duplicidad (brief 1.1/3.1): quién y cuándo registró el abordaje.
    val registeredAt: Long? = null,
    val registeredBy: String? = null,
)

/** Tripulación/proveedores registrados para una frecuencia (brief "R. Tripulación"). */
data class CrewMember(
    val id: String,
    val role: PersonRole,
    val name: String,
    val document: String,
    val phone: String = "",
    val nationality: String = "Perú",
)

/**
 * Resumen que se envía al backend / JESAB-GOP al cerrar el manifiesto de abordaje de una frecuencia.
 * Este es el objeto que, en producción, viaja por la red hacia el backend intermedio (ver README).
 */
data class BoardingSummary(
    val frequencyId: String,
    val boarded: Int,
    val noShow: Int,
    val additional: Int,
    val closedAtEpochMillis: Long,
)
