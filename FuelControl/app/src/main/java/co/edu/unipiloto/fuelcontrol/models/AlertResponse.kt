package co.edu.unipiloto.fuelcontrol.models

import java.math.BigDecimal

data class AlertResponse(
    val id: Long,
    val fuelType: String,
    val levelAtAlert: BigDecimal?,
    val percentageAtAlert: BigDecimal?,
    val thresholdUsed: BigDecimal?,
    val createdAt: String?,
    val read: Boolean
) {
    val isRead: Boolean get() = read
}

