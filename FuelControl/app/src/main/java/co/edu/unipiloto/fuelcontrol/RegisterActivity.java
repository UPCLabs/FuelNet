package co.edu.unipiloto.fuelcontrol;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.Toast;
import android.widget.EditText;
import android.widget.Button;

import api.Client;
import api.requests.AuthResponse;
import api.requests.RegisterResponse;
import retrofit2.Call;


import api.IAuthApi;
import api.requests.RegisterRequest;

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

                Call<RegisterResponse> call =
                        apiService.registerUser(request);

                call.enqueue(new retrofit2.Callback<RegisterResponse>(){

                    @Override
                    public void onResponse(Call<RegisterResponse> call,
                                           retrofit2.Response<RegisterResponse> response) {

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
                    public void onFailure(Call<RegisterResponse> call,
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