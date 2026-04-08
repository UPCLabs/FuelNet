package co.edu.unipiloto.fuelcontrol.api

import co.edu.unipiloto.fuelcontrol.models.FuelPriceDto
import co.edu.unipiloto.fuelcontrol.models.StationDto
import co.edu.unipiloto.fuelcontrol.models.StationPriceResponseDto
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

data class UpdateFuelPriceRequest(
    val fuelType: String,
    val price: Double
)

interface IStationApi {

    @GET("/api/station/get-stations")
    suspend fun getAllStations(): List<StationDto>

    @GET("/api/station/{id}/prices")
    suspend fun getStationPrices(@Path("id") id: Long): StationPriceResponseDto

    @GET("/api/station/prices")
    suspend fun getMyPrices(): List<FuelPriceDto>

    @PUT("/api/station/prices")
    suspend fun updatePrices(
        @Body request: List<UpdateFuelPriceRequest>
    ): Response<Unit>
}