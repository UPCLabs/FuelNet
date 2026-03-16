package co.edu.unipiloto.fuelcontrol.api;

import co.edu.unipiloto.fuelcontrol.api.requests.AuthResponse;
import co.edu.unipiloto.fuelcontrol.api.requests.LoginRequest;
import co.edu.unipiloto.fuelcontrol.api.requests.RegisterRequest;
import co.edu.unipiloto.fuelcontrol.api.requests.RegisterResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;


public interface IAuthApi {
    @POST("/api/auth/register")
    Call<RegisterResponse> registerUser(@Body RegisterRequest request);

    @POST("/api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);
}
