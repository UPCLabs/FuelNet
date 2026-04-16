package co.edu.unipiloto.fuelcontrol;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Button;
import android.content.Intent;
import android.widget.TextView;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.concurrent.Executor;

import co.edu.unipiloto.fuelcontrol.api.Client;
import co.edu.unipiloto.fuelcontrol.api.IAuthApi;
import co.edu.unipiloto.fuelcontrol.api.INotificationApi;
import co.edu.unipiloto.fuelcontrol.api.RegisterTokenRequest;
import co.edu.unipiloto.fuelcontrol.api.requests.AuthResponse;
import co.edu.unipiloto.fuelcontrol.api.requests.LoginRequest;
import co.edu.unipiloto.fuelcontrol.api.requests.MeResponse;
import retrofit2.Call;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    private void sendToken(String fcmToken) {

        INotificationApi api = Client
                .getClient(this)
                .create(INotificationApi.class);

        RegisterTokenRequest request = new RegisterTokenRequest(fcmToken);

        api.registerToken(request).enqueue(new retrofit2.Callback<Void>() {

            @Override
            public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("FCM", "Token registrado en backend");
                } else {
                    Log.e("FCM", "Error registrando token");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("FCM", "Fallo conexión", t);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("FuelControlPrefs", MODE_PRIVATE);

        String token = prefs.getString("token", null);
        String role = prefs.getString("role", null);
        boolean biometriaActiva = prefs.getBoolean("biometria", false);

        if (token != null && !isTokenExpired(token)) {

            if (biometriaActiva) {
                autenticarBiometrico(role);
            } else {
                irAlDashboard(role);
            }

            return;
        }

        if (token != null && isTokenExpired(token)) {
            prefs.edit().clear().apply();
        }

        EditText etCorreo = findViewById(R.id.etCorreo);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegistro = findViewById(R.id.tvRegistro);

        tvRegistro.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> {

            String correo = etCorreo.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this,
                        "Complete todos los campos",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            IAuthApi apiService = Client
                    .getClient(MainActivity.this)
                    .create(IAuthApi.class);

            LoginRequest request = new LoginRequest(correo, password);

            apiService.login(request).enqueue(new retrofit2.Callback<AuthResponse>() {

                @Override
                public void onResponse(retrofit2.Call<AuthResponse> call,
                                       retrofit2.Response<AuthResponse> response) {

                    if (response.isSuccessful() && response.body() != null) {

                        String token = response.body().getToken();
                        String role = response.body().getRole();

                        FirebaseMessaging.getInstance().getToken()
                                .addOnCompleteListener(task -> {
                                    if (!task.isSuccessful()) {
                                        Log.e("FCM", "Error obteniendo token", task.getException());
                                        return;
                                    }

                                    String fcmToken = task.getResult();

                                    Log.d("FCM", "TOKEN: " + fcmToken);

                                    String savedFcm = prefs.getString("fcm_token", null);

                                    if (!fcmToken.equals(savedFcm)) {
                                        sendToken(fcmToken);
                                        prefs.edit().putString("fcm_token", fcmToken).apply();
                                    }
                                });

                        prefs.edit()
                                .putString("token", token)
                                .putString("role", role)
                                .putBoolean("biometria", true)
                                .apply();

                        irAlDashboard(role);

                    } else {
                        Toast.makeText(MainActivity.this,
                                "Credenciales incorrectas",
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<AuthResponse> call,
                                      Throwable t) {

                    Toast.makeText(MainActivity.this,
                            "Error de conexión",
                            Toast.LENGTH_LONG).show();

                    t.printStackTrace();
                }
            });
        });
    }

    private void autenticarBiometrico(String role) {

        Executor executor = ContextCompat.getMainExecutor(this);

        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        irAlDashboard(role);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(MainActivity.this,
                                "Huella incorrecta",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        prefs.edit().clear().apply();
                        Toast.makeText(MainActivity.this,
                                "Autenticación cancelada",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticación biométrica")
                .setSubtitle("Usa tu huella para ingresar")
                .setNegativeButtonText("Cancelar")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void verificarEstacion(String role) {
        IAuthApi apiService = Client.getClient(this).create(IAuthApi.class);

        apiService.getMe().enqueue(new retrofit2.Callback<MeResponse>() {
            @Override
            public void onResponse(Call<MeResponse> call, retrofit2.Response<MeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Integer stationId = response.body().getStationId();

                    Intent intent;
                    if (stationId == null) {
                        intent = new Intent(MainActivity.this, CreateStationActivity.class);
                    } else {
                        intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                    }
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(MainActivity.this,
                            "Error verificando cuenta", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MeResponse> call, Throwable t) {
                System.out.println(t.getMessage());
                Toast.makeText(MainActivity.this,
                        "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Class<?> resolverDashboard(String role) {
        switch (role.toUpperCase()) {
            case "PLATFORM_ADMIN": return SuperAdminDashboardActivity.class;
            case "STATION_ADMIN":  return null;
            default:               return DashboardActivity.class;
        }
    }

    private void irAlDashboard(String role) {
        if (role == null) return;

        if (role.equalsIgnoreCase("STATION_ADMIN")) {
            verificarEstacion(role);
            return;
        }

        Class<?> destino = resolverDashboard(role);
        Intent intent = new Intent(MainActivity.this, destino);
        startActivity(intent);
        finish();
    }

    private boolean isTokenExpired(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return true;

            String payload = parts[1];

            byte[] decoded = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE);
            String json = new String(decoded);

            org.json.JSONObject obj = new org.json.JSONObject(json);
            long exp = obj.getLong("exp");

            long now = System.currentTimeMillis() / 1000;

            return now > exp;

        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }
}