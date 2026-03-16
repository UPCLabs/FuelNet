package co.edu.unipiloto.fuelcontrol.models

data class StationDto(
    val id: Long,
    val name: String,
    val address: String
)

data class StationPriceResponseDto(
    val id: Long,
    val name: String,
    val fuels: List<FuelPriceDto>
)

data class FuelPriceDto(
    val type: String,
    val price: Double
)