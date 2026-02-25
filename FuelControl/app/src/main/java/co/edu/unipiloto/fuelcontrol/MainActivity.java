package co.edu.unipiloto.fuelcontrol;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Button;
import android.content.Intent;
import android.widget.TextView;

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
                    Toast.makeText(MainActivity.this,
                            "Inicio de sesión correcto (simulado)",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}