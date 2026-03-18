package co.edu.unipiloto.fuelcontrol.api.requests

data class CreatePaymentRequest(
    val userEmail: String,
    val fuelType: String,
    val gallons: Double,
    val amount: Double
)