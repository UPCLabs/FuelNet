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
import co.edu.unipiloto.fuelcontrol.api.IPriceApi
import co.edu.unipiloto.fuelcontrol.api.IStationApi
import co.edu.unipiloto.fuelcontrol.api.PriceRegulatedResponse
import co.edu.unipiloto.fuelcontrol.api.requests.CreateStationRequest
import co.edu.unipiloto.fuelcontrol.api.requests.FuelItem
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale
import android.text.Editable
import android.text.TextWatcher

class CreateStationActivity : AppCompatActivity() {

    private lateinit var etNombreEstacion: TextInputEditText
    private lateinit var etDireccionEstacion: TextInputEditText
    private lateinit var cbCorriente: CheckBox
    private lateinit var cbExtra: CheckBox
    private lateinit var cbDiesel: CheckBox
    private lateinit var tilPrecioCorriente: TextInputLayout
    private lateinit var tilPrecioExtra: TextInputLayout
    private lateinit var tilPrecioDiesel: TextInputLayout
    private lateinit var tvRefCorriente: TextView
    private lateinit var tvRefDiesel: TextView
    private lateinit var btnCrearEstacion: MaterialButton
    private lateinit var prefs: SharedPreferences

    private var refCorriente: Int? = null
    private var refDiesel: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_station)

        prefs = getSharedPreferences("FuelControlPrefs", MODE_PRIVATE)

        etNombreEstacion    = findViewById(R.id.etNombreEstacion)
        etDireccionEstacion = findViewById(R.id.etDireccionEstacion)
        cbCorriente         = findViewById(R.id.cbCorriente)
        tilPrecioCorriente  = findViewById(R.id.tilPrecioCorriente)
        cbExtra             = findViewById(R.id.cbExtra)
        tilPrecioExtra      = findViewById(R.id.tilPrecioExtra)
        cbDiesel            = findViewById(R.id.cbDiesel)
        tilPrecioDiesel     = findViewById(R.id.tilPrecioDiesel)
        tvRefCorriente      = findViewById(R.id.tvRefCorriente)
        tvRefDiesel         = findViewById(R.id.tvRefDiesel)
        btnCrearEstacion    = findViewById(R.id.btnCrearEstacion)

        cbCorriente.setOnCheckedChangeListener { _, checked ->
            tilPrecioCorriente.visibility = if (checked) View.VISIBLE else View.GONE
            if (checked) {
                refCorriente?.let {
                    tvRefCorriente.visibility = View.VISIBLE
                    actualizarBadge(tvRefCorriente, "Corriente", it, null)
                }
            } else {
                tvRefCorriente.visibility = View.GONE
                tilPrecioCorriente.error = null
            }
            validarPrecios()
        }

        cbExtra.setOnCheckedChangeListener { _, checked ->
            tilPrecioExtra.visibility = if (checked) View.VISIBLE else View.GONE
        }

        cbDiesel.setOnCheckedChangeListener { _, checked ->
            tilPrecioDiesel.visibility = if (checked) View.VISIBLE else View.GONE
            if (checked) {
                refDiesel?.let {
                    tvRefDiesel.visibility = View.VISIBLE
                    actualizarBadge(tvRefDiesel, "ACPM", it, null)
                }
            } else {
                tvRefDiesel.visibility = View.GONE
                tilPrecioDiesel.error = null
            }
            validarPrecios()
        }

        tilPrecioCorriente.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = validarPrecios()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        tilPrecioDiesel.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = validarPrecios()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnCrearEstacion.setOnClickListener { validarYCrear() }

        cargarPreciosReferencia()
    }

    private fun cargarPreciosReferencia() {
        val api = Client.getClient(this).create(IPriceApi::class.java)
        api.getCurrentPrices().enqueue(object : Callback<PriceRegulatedResponse> {
            override fun onResponse(
                call: Call<PriceRegulatedResponse>,
                response: Response<PriceRegulatedResponse>
            ) {
                if (!response.isSuccessful) {
                    Log.e("PRICES", "Error HTTP ${response.code()}")
                    return
                }
                val body = response.body() ?: return

                refCorriente = body.corriente
                refDiesel    = body.diesel

                if (cbCorriente.isChecked) {
                    tvRefCorriente.visibility = View.VISIBLE
                    actualizarBadge(tvRefCorriente, "Corriente", body.corriente, null)
                }
                if (cbDiesel.isChecked) {
                    tvRefDiesel.visibility = View.VISIBLE
                    actualizarBadge(tvRefDiesel, "ACPM", body.diesel, null)
                }
            }

            override fun onFailure(call: Call<PriceRegulatedResponse>, t: Throwable) {
                Log.e("PRICES", "No se pudo cargar precio referencia: ${t.message}")
            }
        })
    }

    private fun validarPrecios() {
        var hayError = false

        if (cbCorriente.isChecked) {
            val input = tilPrecioCorriente.editText?.text.toString().toDoubleOrNull()
            val ref   = refCorriente
            when {
                ref == null -> {
                    // Sin precio de referencia aún, no validar
                    tilPrecioCorriente.error = null
                }
                input == null -> {
                    // Campo vacío
                    tilPrecioCorriente.error = null
                    actualizarBadge(tvRefCorriente, "Corriente", ref, null)
                }
                input > ref -> {
                    hayError = true
                    tilPrecioCorriente.error = "Supera el precio regulado por la CREG"
                    actualizarBadge(tvRefCorriente, "Corriente", ref, true)
                }
                else -> {
                    tilPrecioCorriente.error = null
                    actualizarBadge(tvRefCorriente, "Corriente", ref, false)
                }
            }
        }

        if (cbDiesel.isChecked) {
            val input = tilPrecioDiesel.editText?.text.toString().toDoubleOrNull()
            val ref   = refDiesel
            when {
                ref == null -> {
                    tilPrecioDiesel.error = null
                }
                input == null -> {
                    tilPrecioDiesel.error = null
                    actualizarBadge(tvRefDiesel, "ACPM", ref, null)
                }
                input > ref -> {
                    hayError = true
                    tilPrecioDiesel.error = "Supera el precio regulado por la CREG"
                    actualizarBadge(tvRefDiesel, "ACPM", ref, true)
                }
                else -> {
                    tilPrecioDiesel.error = null
                    actualizarBadge(tvRefDiesel, "ACPM", ref, false)
                }
            }
        }

        btnCrearEstacion.isEnabled = !hayError
        btnCrearEstacion.alpha     = if (hayError) 0.5f else 1.0f
    }

    private fun actualizarBadge(tv: TextView, label: String, ref: Int, supera: Boolean?) {
        val formato = NumberFormat.getNumberInstance(Locale("es", "CO"))
        tv.text = "📋 Precio CREG ($label): $${formato.format(ref)}/gal"
        tv.visibility = View.VISIBLE  // siempre visible cuando se llama

        when (supera) {
            true -> {
                tv.setTextColor(getColor(android.R.color.holo_red_dark))
                tv.setBackgroundResource(R.drawable.bg_price_badge_error)
            }
            false -> {
                tv.setTextColor(getColor(android.R.color.holo_green_dark))
                tv.setBackgroundResource(R.drawable.bg_price_badge_ok)
            }
            null -> {
                tv.setTextColor(getColor(android.R.color.darker_gray))
                tv.setBackgroundResource(R.drawable.bg_price_badge)
            }
        }
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

        val api     = Client.getClient(this).create(IStationApi::class.java)
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
        dialogView.findViewById<TextView>(R.id.tvDialogMensaje).text =
            "Tu estación fue registrada correctamente. Ya puedes comenzar a operar."

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<MaterialButton>(R.id.btnDialogAceptar).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, AdminDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

        dialog.show()
    }
}