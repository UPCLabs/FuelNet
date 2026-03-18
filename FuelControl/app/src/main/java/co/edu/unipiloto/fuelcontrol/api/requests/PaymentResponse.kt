package co.edu.unipiloto.fuelcontrol.api.requests

data class PaymentResponse(
    val id: Long,
    val clientName: String,
    val clientEmail: String,
    val fuelType: String,
    val gallons: Double,
    val amount: Double,
    val status: String,
    val createdAt: String,
    val paidAt: String?
)