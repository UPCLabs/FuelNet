package co.edu.unipiloto.fuelcontrol.api

import co.edu.unipiloto.fuelcontrol.models.StationDto
import co.edu.unipiloto.fuelcontrol.models.StationPriceResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

interface IStationApi {
    @GET("/api/station/get-stations")
    suspend fun getAllStations(): List<StationDto>

    @GET("/api/station/{id}/prices")
    suspend fun getStationPrices(@Path("id") id: Long): StationPriceResponseDto
}