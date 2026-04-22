package co.edu.unipiloto.fuelcontrol.api;

import co.edu.unipiloto.fuelcontrol.api.requests.AuthResponse;
import co.edu.unipiloto.fuelcontrol.api.requests.ChangePasswordRequest;
import co.edu.unipiloto.fuelcontrol.api.requests.CreateEmployeeRequest;
import co.edu.unipiloto.fuelcontrol.api.requests.LoginRequest;
import co.edu.unipiloto.fuelcontrol.api.requests.MeResponse;
import co.edu.unipiloto.fuelcontrol.api.requests.RegisterRequest;
import co.edu.unipiloto.fuelcontrol.api.requests.RegisterResponse;
import co.edu.unipiloto.fuelcontrol.models.PaymentSummaryResponse;
import kotlin.Unit;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import java.util.List;
import co.edu.unipiloto.fuelcontrol.api.requests.CreatePaymentRequest;
import co.edu.unipiloto.fuelcontrol.models.AlertResponse;
import retrofit2.http.PATCH;
import retrofit2.http.Path;

public interface IAuthApi {
    @POST("/api/auth/register")
    Call<RegisterResponse> registerUser(@Body RegisterRequest request);

    @POST("/api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @GET("/api/payments/my-payments")
    Call<List<PaymentSummaryResponse>> getMyPayments(
            @Header("Authorization") String token
    );
    @POST("/api/payments/create")
    Call<PaymentSummaryResponse> createPayment(
            @Header("Authorization") String token,
            @Body CreatePaymentRequest request
    );

    @POST("/api/users/change-password")
    Call<Response<Unit>> changePassword(
            @Body ChangePasswordRequest request
    );

    @GET("/api/alerts")
    Call<List<AlertResponse>> getAlerts(
            @Header("Authorization") String token
    );

    @GET("/api/auth/me")
    Call<MeResponse> getMe();

    @PATCH("/api/alerts/{id}/read")
    Call<Void> markAsRead(
            @Header("Authorization") String token,
            @Path("id") Long id
    );

    @POST("/api/auth/create-employee")
    Call<Void> createEmployee(
            @Body CreateEmployeeRequest request
    );
}