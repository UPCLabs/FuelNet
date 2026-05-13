package co.edu.unipiloto.fuelcontrol.api

import retrofit2.Call
import retrofit2.http.GET

// PriceRegulatedResponse.kt
data class PriceRegulatedResponse(
    val id: Long,
    val document: String,
    val url: String,
    val corriente: Int,
    val diesel: Int,
    val fetchedAt: String
)

// IPriceApi.kt
interface IPriceApi {
    @GET("api/regulated-prices/current")
    fun getCurrentPrices(): Call<PriceRegulatedResponse>
}