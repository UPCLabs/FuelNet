package co.edu.unipiloto.fuelcontrol;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Button;
import android.content.Intent;
import android.widget.TextView;

import java.util.concurrent.Executor;

import co.edu.unipiloto.fuelcontrol.api.Client;
import co.edu.unipiloto.fuelcontrol.api.IAuthApi;
import co.edu.unipiloto.fuelcontrol.api.requests.AuthResponse;
import co.edu.unipiloto.fuelcontrol.api.requests.LoginRequest;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences prefs;

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

    private void irAlDashboard(String role) {
        Intent intent;

        if (role != null && role.equalsIgnoreCase("STATION_ADMIN")) {
            intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
        } else {
            intent = new Intent(MainActivity.this, DashboardActivity.class);
        }

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