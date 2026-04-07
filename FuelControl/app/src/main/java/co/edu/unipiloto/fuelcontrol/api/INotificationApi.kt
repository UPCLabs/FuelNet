package co.edu.unipiloto.fuelcontrol.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class RegisterTokenRequest(
    val token: String
)

interface INotificationApi {
    @POST("/api/notification/register")
    fun registerToken(
        @Body request: RegisterTokenRequest
    ): Call<Void>
}