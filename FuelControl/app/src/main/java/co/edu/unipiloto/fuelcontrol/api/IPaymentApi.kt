package co.edu.unipiloto.fuelcontrol.api

import co.edu.unipiloto.fuelcontrol.api.requests.CreatePaymentRequest
import co.edu.unipiloto.fuelcontrol.api.requests.PaymentResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface IPaymentApi {

    @GET("/api/payments/my")
    fun getMyPayments(
        @Header("Authorization") token: String
    ): Call<List<PaymentResponse>>

    @GET("/api/payments/{id}/summary")
    fun getPaymentSummary(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Call<PaymentResponse>

    @POST("/api/payments/{id}/pay")
    fun payPayment(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Call<PaymentResponse>

    @POST("/api/payments")
    fun createPayment(
        @Header("Authorization") token: String,
        @Body request: CreatePaymentRequest
    ): Call<PaymentResponse>

    @GET("/api/payments/admin")
    fun getAdminPayments(
        @Header("Authorization") token: String
    ): Call<List<PaymentResponse>>
}