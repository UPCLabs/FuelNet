package co.edu.unipiloto.fuelcontrol.models

import com.google.gson.annotations.SerializedName

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
    @SerializedName("type")
    val fuelType: String,
    val price: Double
)