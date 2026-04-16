package co.edu.unipiloto.fuelcontrol.api

import co.edu.unipiloto.fuelcontrol.PendingUserDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface IPendingUserApi {

    @GET("api/pending-users/get-pending")
    fun getPendingUsers(): Call<List<PendingUserDto>>

    @POST("api/auth/admin_aceptation")
    fun resolveUser(@Body request: AdminAceptationRequest): Call<Void>
}
data class AdminAceptationRequest(
    val pendingUserId: Long,
    val accepted: Boolean,
    val roleRequested: String
)