package co.edu.unipiloto.fuelcontrol.api.requests

data class CreateStationRequest(
    val name: String,
    val address: String,
    val fuels: List<FuelItem>
)

data class FuelItem(
    val type: String,
    val price: Double
)