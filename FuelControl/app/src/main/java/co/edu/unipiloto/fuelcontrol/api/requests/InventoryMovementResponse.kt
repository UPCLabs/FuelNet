package co.edu.unipiloto.fuelcontrol.api.requests

data class InventoryMovementResponse(
    val id: Long,
    val fuelType: String,
    val gallonsAdded: Double,
    val levelBefore: Double,
    val levelAfter: Double,
    val fillPercentageAfter: Double,
    val supplier: String?,
    val rechargeDate: String,
    val notes: String?,
    val registeredBy: String
)