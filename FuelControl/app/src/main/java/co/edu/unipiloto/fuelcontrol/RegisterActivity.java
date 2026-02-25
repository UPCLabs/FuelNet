package co.edu.unipiloto.fuelcontrol;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.Toast;
import android.widget.EditText;
import android.widget.Button;

import api.Client;
import requests.AuthResponse;
import retrofit2.Call;


import api.IAuthApi;
import requests.RegisterRequest;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText etNombre = findViewById(R.id.etNombre);
        EditText etCorreo = findViewById(R.id.etCorreoRegistro);
        EditText etPassword = findViewById(R.id.etPasswordRegistro);
        EditText etConfirmar = findViewById(R.id.etConfirmarPassword);
        Button btnRegistrar = findViewById(R.id.btnRegistrar);

        btnRegistrar.setOnClickListener(v -> {

            String nombre = etNombre.getText().toString().trim();
            String correo = etCorreo.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmar = etConfirmar.getText().toString().trim();

            if(nombre.isEmpty() || correo.isEmpty() || password.isEmpty() || confirmar.isEmpty()){
                Toast.makeText(RegisterActivity.this,
                        "Complete todos los campos",
                        Toast.LENGTH_SHORT).show();
            }
            else if(!password.equals(confirmar)){
                Toast.makeText(RegisterActivity.this,
                        "Las contraseñas no coinciden",
                        Toast.LENGTH_SHORT).show();
            }
            else {

                IAuthApi apiService = Client
                        .getClient()
                        .create(IAuthApi.class);

                RegisterRequest request =
                        new RegisterRequest(nombre, correo, password);

                Call<AuthResponse> call =
                        apiService.registerUser(request);

                call.enqueue(new retrofit2.Callback<AuthResponse>() {

                    @Override
                    public void onResponse(Call<AuthResponse> call,
                                           retrofit2.Response<AuthResponse> response) {

                        if(response.isSuccessful() && response.body() != null){

                            Toast.makeText(RegisterActivity.this,
                                    response.body().getMessage(),
                                    Toast.LENGTH_LONG).show();

                            finish(); // vuelve al login
                        }
                        else{
                            Toast.makeText(RegisterActivity.this,
                                    "Error en el registro",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call,
                                          Throwable t) {

                        Toast.makeText(RegisterActivity.this,
                                "Error de conexión",
                                Toast.LENGTH_LONG).show();

                        t.printStackTrace();
                    }
                });
            }

        });
    }
}