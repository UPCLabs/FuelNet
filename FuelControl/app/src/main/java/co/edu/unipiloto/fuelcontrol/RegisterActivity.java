package co.edu.unipiloto.fuelcontrol;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.Toast;
import android.widget.EditText;
import android.widget.Button;

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
            else{
                Toast.makeText(RegisterActivity.this,
                        "Registro exitoso",
                        Toast.LENGTH_SHORT).show();

                finish(); // vuelve al login
            }

        });
    }
}