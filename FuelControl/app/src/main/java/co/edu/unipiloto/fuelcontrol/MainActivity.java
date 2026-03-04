package co.edu.unipiloto.fuelcontrol;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Button;
import android.content.Intent;
import android.widget.TextView;

import co.edu.unipiloto.fuelcontrol.api.Client;
import co.edu.unipiloto.fuelcontrol.api.IAuthApi;
import co.edu.unipiloto.fuelcontrol.api.requests.AuthResponse;
import co.edu.unipiloto.fuelcontrol.api.requests.LoginRequest;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText etCorreo = findViewById(R.id.etCorreo);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegistro = findViewById(R.id.tvRegistro);


        tvRegistro.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String correo = etCorreo.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (correo.isEmpty() || password.isEmpty()) {
                    Toast.makeText(MainActivity.this,
                            "Complete todos los campos",
                            Toast.LENGTH_SHORT).show();
                } else {

                    IAuthApi apiService = Client
                            .getClient(MainActivity.this)
                            .create(IAuthApi.class);

                    LoginRequest request = new LoginRequest(correo, password);

                    apiService.login(request).enqueue(new retrofit2.Callback<AuthResponse>() {

                        @Override
                        public void onResponse(retrofit2.Call<AuthResponse> call,
                                               retrofit2.Response<AuthResponse> response) {

                            if(response.isSuccessful() && response.body() != null){

                                String token = response.body().getToken();

                                SharedPreferences prefs = getSharedPreferences("FuelControlPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("token", token);
                                editor.apply();


                                Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                                startActivity(intent);
                                finish();

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
                }
            }
        });
    }
}