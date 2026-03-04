package api;

import api.requests.AuthResponse;
import api.requests.LoginRequest;
import api.requests.RegisterRequest;
import api.requests.RegisterResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;


public interface IAuthApi {
    @POST("api/auth/register")
    Call<RegisterResponse> registerUser(@Body RegisterRequest request);

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);
}
