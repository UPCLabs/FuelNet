package co.edu.unipiloto.fuelcontrol

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import co.edu.unipiloto.fuelcontrol.api.Client
import co.edu.unipiloto.fuelcontrol.api.IStationApi
import co.edu.unipiloto.fuelcontrol.api.requests.CreateStationRequest
import co.edu.unipiloto.fuelcontrol.api.requests.FuelItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateStationActivity : AppCompatActivity() {

    private lateinit var etNombreEstacion: TextInputEditText
    private lateinit var etDireccionEstacion: TextInputEditText
    private lateinit var cbCorriente: CheckBox

    private lateinit var tilPrecioCorriente: com.google.android.material.textfield.TextInputLayout
    private lateinit var tilPrecioExtra: com.google.android.material.textfield.TextInputLayout
    private lateinit var tilPrecioDiesel: com.google.android.material.textfield.TextInputLayout

    private lateinit var cbExtra: CheckBox
    private lateinit var cbDiesel: CheckBox
    private lateinit var btnCrearEstacion: MaterialButton
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_station)

        prefs = getSharedPreferences("FuelControlPrefs", MODE_PRIVATE)

        etNombreEstacion    = findViewById(R.id.etNombreEstacion)
        etDireccionEstacion = findViewById(R.id.etDireccionEstacion)
        cbCorriente         = findViewById(R.id.cbCorriente)
        tilPrecioCorriente   = findViewById(R.id.tilPrecioCorriente)
        cbExtra             = findViewById(R.id.cbExtra)
        tilPrecioExtra       = findViewById(R.id.tilPrecioExtra)
        cbDiesel            = findViewById(R.id.cbDiesel)
        tilPrecioDiesel      = findViewById(R.id.tilPrecioDiesel)
        btnCrearEstacion    = findViewById(R.id.btnCrearEstacion)

        // Mostrar/ocultar precio según checkbox
        cbCorriente.setOnCheckedChangeListener { _, checked ->
            tilPrecioCorriente.visibility = if (checked) View.VISIBLE else View.GONE
        }
        cbExtra.setOnCheckedChangeListener { _, checked ->
            tilPrecioExtra.visibility = if (checked) View.VISIBLE else View.GONE
        }
        cbDiesel.setOnCheckedChangeListener { _, checked ->
            tilPrecioDiesel.visibility = if (checked) View.VISIBLE else View.GONE
        }

        btnCrearEstacion.setOnClickListener { validarYCrear() }
    }

    private fun validarYCrear() {
        val nombre    = etNombreEstacion.text.toString().trim()
        val direccion = etDireccionEstacion.text.toString().trim()

        if (nombre.isEmpty() || direccion.isEmpty()) {
            Toast.makeText(this, "Completa nombre y dirección", Toast.LENGTH_SHORT).show()
            return
        }

        val fuels = mutableListOf<FuelItem>()

        if (cbCorriente.isChecked) {
            val precio = tilPrecioCorriente.editText?.text.toString().trim()
            if (precio.isEmpty()) {
                Toast.makeText(this, "Ingresa el precio de Corriente", Toast.LENGTH_SHORT).show()
                return
            }
            fuels.add(FuelItem("Corriente", precio.toDouble()))
        }

        if (cbExtra.isChecked) {
            val precio = tilPrecioExtra.editText?.text.toString().trim()
            if (precio.isEmpty()) {
                Toast.makeText(this, "Ingresa el precio de Extra", Toast.LENGTH_SHORT).show()
                return
            }
            fuels.add(FuelItem("Extra", precio.toDouble()))
        }

        if (cbDiesel.isChecked) {
            val precio = tilPrecioDiesel.editText?.text.toString().trim()
            if (precio.isEmpty()) {
                Toast.makeText(this, "Ingresa el precio de Diesel", Toast.LENGTH_SHORT).show()
                return
            }
            fuels.add(FuelItem("Diesel", precio.toDouble()))
        }

        if (fuels.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos un tipo de combustible", Toast.LENGTH_SHORT).show()
            return
        }

        val token = prefs.getString("token", null)
        val api   = Client.getClient(this).create(IStationApi::class.java)
        val request = CreateStationRequest(nombre, direccion, fuels)

        api.createStation(request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    mostrarDialogoExito()
                } else {
                    var error = "Error ${response.code()}"
                    try {
                        response.errorBody()?.let { error += ": ${it.string()}" }
                    } catch (e: Exception) {
                        Log.e("CREATE_STATION", "No se pudo leer el error", e)
                    }
                    Log.e("CREATE_STATION", error)
                    Toast.makeText(this@CreateStationActivity, error, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(
                    this@CreateStationActivity,
                    "Error de conexión: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun mostrarDialogoExito() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_registro_exitoso, null)

        dialogView.findViewById<TextView>(R.id.tvDialogTitulo).text  = "¡Estación creada!"
        dialogView.findViewById<TextView>(R.id.tvDialogMensaje).text = "Tu estación fue registrada correctamente. Ya puedes comenzar a operar."

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<MaterialButton>(R.id.btnDialogAceptar).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, AdminDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }

        dialog.show()
    }
}