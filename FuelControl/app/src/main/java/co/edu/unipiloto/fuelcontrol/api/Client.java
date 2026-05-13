package co.edu.unipiloto.fuelcontrol.api;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Client {

    private static final String BASE_URL = "http://192.168.0.7:3015/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {

            Interceptor authInterceptor = chain -> {
                Request original = chain.request();
                String url = original.url().encodedPath();

                if (
                        url.equals("/api/auth/login") ||
                                url.equals("/api/auth/register")
                ) {
                    return chain.proceed(original);
                }

                SharedPreferences prefs = context.getSharedPreferences("FuelControlPrefs", Context.MODE_PRIVATE);
                String token = prefs.getString("token", "");

                if (!token.isEmpty()) {
                    Request newRequest = original.newBuilder()
                            .header("Authorization", "Bearer " + token)
                            .build();
                    return chain.proceed(newRequest);
                }

                return chain.proceed(original);
            };

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .addInterceptor(authInterceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}