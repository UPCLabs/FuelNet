package co.edu.unipiloto.fuelcontrol.api

import co.edu.unipiloto.fuelcontrol.api.requests.FuelTankResponse
import co.edu.unipiloto.fuelcontrol.api.requests.InventoryMovementResponse
import co.edu.unipiloto.fuelcontrol.api.requests.RechargeRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface IInventoryApi {

    @GET("/api/inventory/dashboard")
    fun getDashboard(
        @Header("Authorization") token: String
    ): Call<List<FuelTankResponse>>

    @POST("/api/inventory/recharge")
    fun recharge(
        @Header("Authorization") token: String,
        @Body request: RechargeRequest
    ): Call<InventoryMovementResponse>

    @GET("/api/inventory/history")
    fun getHistory(
        @Header("Authorization") token: String
    ): Call<List<InventoryMovementResponse>>
}