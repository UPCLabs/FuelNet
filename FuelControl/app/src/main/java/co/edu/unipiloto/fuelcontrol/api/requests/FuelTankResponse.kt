package co.edu.unipiloto.fuelcontrol.api.requests

data class FuelTankResponse(
    val id: Long,
    val fuelType: String,
    val capacityGallons: Double,
    val currentLevelGallons: Double,
    val fillPercentage: Double,
    val lastUpdated: String
)