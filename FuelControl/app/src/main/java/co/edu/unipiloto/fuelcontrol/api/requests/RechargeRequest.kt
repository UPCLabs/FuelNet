package co.edu.unipiloto.fuelcontrol.api.requests

data class RechargeRequest(
    val fuelType: String,
    val gallonsAdded: Double,
    val supplier: String,
    val rechargeDate: String? = null,
    val notes: String? = null
)