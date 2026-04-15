package co.edu.unipiloto.fuelcontrol;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.app.DatePickerDialog;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;
import co.edu.unipiloto.fuelcontrol.api.Client;
import co.edu.unipiloto.fuelcontrol.api.IAuthApi;
import co.edu.unipiloto.fuelcontrol.api.requests.RegisterRequest;
import co.edu.unipiloto.fuelcontrol.api.requests.RegisterResponse;
import retrofit2.Call;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    TextInputEditText etNombre, etUsuario, etCorreo, etDireccion, etPassword, etConfirmar, etFecha;
    Spinner spinnerRol;
    RadioGroup radioGenero;
    Button btnRegistrar, btnUbicacion;
    FusedLocationProviderClient fusedLocationClient;
    int anioNacimiento = 0, mesNacimiento = 0, diaNacimiento = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etNombre = findViewById(R.id.etNombre);
        etUsuario = findViewById(R.id.etUsuario);
        etCorreo = findViewById(R.id.etCorreoRegistro);
        etDireccion = findViewById(R.id.etDireccion);
        etPassword = findViewById(R.id.etPasswordRegistro);
        etConfirmar = findViewById(R.id.etConfirmarPassword);
        etFecha = findViewById(R.id.etFechaNacimiento);
        spinnerRol = findViewById(R.id.spinnerRol);
        radioGenero = findViewById(R.id.radioGroupGenero);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        btnUbicacion = findViewById(R.id.btnUbicacion);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        ArrayAdapter<String> adapterRol = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Usuario", "Administrador de estacion"});
        adapterRol.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRol.setAdapter(adapterRol);


        etFecha.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                anioNacimiento = year;
                mesNacimiento = month + 1;
                diaNacimiento = day;
                etFecha.setText(day + "/" + (month + 1) + "/" + year);
            }, cal.get(Calendar.YEAR) - 18, cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });


        btnUbicacion.setOnClickListener(v -> obtenerUbicacion());


        btnRegistrar.setOnClickListener(v -> validarYRegistrar());
    }

    private void obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                try {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        etDireccion.setText(addresses.get(0).getAddressLine(0));
                    } else {
                        etDireccion.setText(location.getLatitude() + ", " + location.getLongitude());
                    }
                } catch (Exception e) {
                    etDireccion.setText(location.getLatitude() + ", " + location.getLongitude());
                }
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void mostrarDialogoExito() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_registro_exitoso, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnDialogAceptar).setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }
    private void validarYRegistrar() {
        String nombre = etNombre.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmar = etConfirmar.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();
        String fecha = etFecha.getText().toString().trim();
        String usuario = etUsuario.getText() != null ? etUsuario.getText().toString().trim() : "";


        if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty() ||
                confirmar.isEmpty() || direccion.isEmpty() || fecha.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }


        if (!password.equals(confirmar)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }


        if (radioGenero.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Selecciona un género", Toast.LENGTH_SHORT).show();
            return;
        }


        Calendar hoy = Calendar.getInstance();
        int edad = hoy.get(Calendar.YEAR) - anioNacimiento;
        if (mesNacimiento > hoy.get(Calendar.MONTH) + 1 ||
                (mesNacimiento == hoy.get(Calendar.MONTH) + 1 &&
                        diaNacimiento > hoy.get(Calendar.DAY_OF_MONTH))) {
            edad--;
        }
        if (edad < 18) {
            Toast.makeText(this, "Debes ser mayor de 18 años", Toast.LENGTH_LONG).show();
            return;
        }


        String rol = spinnerRol.getSelectedItem().toString();


        String genero = "";
        int generoId = radioGenero.getCheckedRadioButtonId();
        if (generoId == R.id.rbMasculino) genero = "Masculino";
        else if (generoId == R.id.rbFemenino) genero = "Femenino";
        else if (generoId == R.id.rbBinario) genero = "Binario";


        String fechaFormateada = anioNacimiento + "-" +
                String.format("%02d", mesNacimiento) + "-" +
                String.format("%02d", diaNacimiento);

        IAuthApi apiService = Client.getClient(this).create(IAuthApi.class);
        RegisterRequest request = new RegisterRequest(
                nombre,
                usuario,
                correo,
                password,
                direccion,
                fechaFormateada,
                rol,
                genero
        );

        apiService.registerUser(request).enqueue(new retrofit2.Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, retrofit2.Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    mostrarDialogoExito();
                } else {
                    // Leer el cuerpo del error
                    String errorMsg = "Error " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += ": " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        errorMsg += " (no se pudo leer el error)";
                    }
                    Log.e("REGISTER", errorMsg);
                    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                String errorMsg = "Error de conexión: " + t.getMessage();
                Log.e("REGISTER", errorMsg, t);
                Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }
}