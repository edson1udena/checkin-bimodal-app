package com.incarail.checkinbimodal.data

/**
 * Modelos de dominio. Estos mismos modelos se llenarán, en la fase de integración,
 * con la respuesta real de la API de ventas de Inca Rail en lugar de los datos de ejemplo
 * de [MockPassengerRepository].
 */

enum class BoardingStatus { PENDING, BOARDED, NO_SHOW }

enum class FrequencyStatus { PENDING, IN_PROGRESS, CLOSED }

data class Frequency(
    val id: String,
    val time: String,
    val route: String,
    val unit: String,
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
