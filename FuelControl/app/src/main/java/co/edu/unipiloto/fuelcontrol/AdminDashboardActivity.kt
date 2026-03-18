package co.edu.unipiloto.fuelcontrol

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.edu.unipiloto.fuelcontrol.api.Client
import co.edu.unipiloto.fuelcontrol.api.IAuthApi
import co.edu.unipiloto.fuelcontrol.api.requests.CreatePaymentRequest
import co.edu.unipiloto.fuelcontrol.models.PaymentSummaryResponse
import co.edu.unipiloto.fuelcontrol.ui.theme.FuelControlTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

enum class AdminDestinations(
    val label: String,
    val icon: ImageVector
) {
    FACTURAS("Facturas", Icons.Default.Description),
    INVENTARIO("Inventario", Icons.Default.Inventory)
}

class AdminDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FuelControlTheme {
                AdminDashboardScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen() {
    val context = LocalContext.current

    var currentDestination by remember { mutableStateOf(AdminDestinations.FACTURAS) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AdminDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AdminDestinations.FACTURAS -> FacturasScreen(modifier = Modifier.padding(innerPadding))
                AdminDestinations.INVENTARIO -> InventarioScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacturasScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("FuelControlPrefs", Context.MODE_PRIVATE)
    val token = "Bearer " + (prefs.getString("token", "") ?: "")

    var email by remember { mutableStateOf("") }
    var cedula by remember { mutableStateOf("") }
    var galones by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var combustibleSeleccionado by remember { mutableStateOf("CORRIENTE") }
    var expandedSpinner by remember { mutableStateOf(false) }
    var tvEstado by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var facturas by remember { mutableStateOf<List<PaymentSummaryResponse>>(emptyList()) }

    val combustibles = listOf("CORRIENTE", "EXTRA", "DIESEL", "PREMIUM")

    LaunchedEffect(Unit) {
        val api = Client.getClient(context).create(IAuthApi::class.java)
        api.getMyPayments(token).enqueue(object : Callback<List<PaymentSummaryResponse>> {
            override fun onResponse(call: Call<List<PaymentSummaryResponse>>, response: Response<List<PaymentSummaryResponse>>) {
                if (response.isSuccessful && response.body() != null) facturas = response.body()!!
            }
            override fun onFailure(call: Call<List<PaymentSummaryResponse>>, t: Throwable) {
                Toast.makeText(context, "Error cargando facturas", Toast.LENGTH_SHORT).show()
            }
        })
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Panel Administrador", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo del cliente") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = cedula, onValueChange = { cedula = it }, label = { Text("Cédula del cliente") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(expanded = expandedSpinner, onExpandedChange = { expandedSpinner = !expandedSpinner }) {
                OutlinedTextField(
                    value = combustibleSeleccionado, onValueChange = {}, readOnly = true,
                    label = { Text("Tipo de combustible") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSpinner) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                )
                ExposedDropdownMenu(expanded = expandedSpinner, onDismissRequest = { expandedSpinner = false }) {
                    combustibles.forEach { tipo ->
                        DropdownMenuItem(text = { Text(tipo) }, onClick = { combustibleSeleccionado = tipo; expandedSpinner = false })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = galones, onValueChange = { galones = it }, label = { Text("Galones") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = monto, onValueChange = { monto = it }, label = { Text("Monto ($)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            if (tvEstado.isNotEmpty()) {
                Text(text = tvEstado, color = if (tvEstado.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (email.isEmpty() || cedula.isEmpty() || galones.isEmpty() || monto.isEmpty()) { tvEstado = "Completa todos los campos"; return@Button }
                    isLoading = true
                    val api = Client.getClient(context).create(IAuthApi::class.java)
                    val request = CreatePaymentRequest(email, combustibleSeleccionado, galones.toDouble(), monto.toDouble())
                    api.createPayment(token, request).enqueue(object : Callback<PaymentSummaryResponse> {
                        override fun onResponse(call: Call<PaymentSummaryResponse>, response: Response<PaymentSummaryResponse>) {
                            isLoading = false
                            if (response.isSuccessful && response.body() != null) {
                                tvEstado = "✓ Factura creada. Notificación enviada al cliente."
                                facturas = listOf(response.body()!!) + facturas
                                email = ""; cedula = ""; galones = ""; monto = ""
                            } else { tvEstado = "Error al crear la factura" }
                        }
                        override fun onFailure(call: Call<PaymentSummaryResponse>, t: Throwable) { isLoading = false; tvEstado = "Error de conexión" }
                    })
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Crear Factura y Notificar")
            }

            Spacer(Modifier.height(16.dp))
            Text("Facturas registradas", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }

        items(facturas) { factura ->
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(factura.message ?: "", fontWeight = FontWeight.Bold)
                        Text("${factura.fuelType} • ${factura.gallons} gal • $${factura.amount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(text = factura.status ?: "", color = if (factura.status == "PENDIENTE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun InventarioScreen(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Niveles", "Recargas", "Historial")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> NivelesTab()
            1 -> RecargasTab()
            2 -> HistorialTab()
        }
    }
}

// ── MODELOS LOCALES mendiz los reemplaza con back

data class TanqueUi(
    val tipo: String,
    val porcentaje: Float,
    val capacidadTotal: Int,
    val actual: Int
)

data class RecargaUi(
    val tipo: String,
    val proveedor: String,
    val cantidad: Int,
    val fecha: String
)

// PESTAÑA 1: NIVELES

@Composable
fun NivelesTab() {
    // Datos de ejemplo mendiz reemplaza con back
    val tanques = listOf(
        TanqueUi("CORRIENTE", 0.72f, 10000, 7200),
        TanqueUi("EXTRA", 0.45f, 8000, 3600),
        TanqueUi("DIESEL", 0.88f, 12000, 10560),
        TanqueUi("PREMIUM", 0.20f, 5000, 1000),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tanques) { tanque ->
            TanqueCard(tanque)
        }
    }
}

@Composable
fun TanqueCard(tanque: TanqueUi) {
    val color = when {
        tanque.porcentaje >= 0.6f -> MaterialTheme.colorScheme.primary
        tanque.porcentaje >= 0.3f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tanque.tipo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("${tanque.actual} / ${tanque.capacidadTotal} gal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        tanque.porcentaje >= 0.6f -> "✓ Nivel normal"
                        tanque.porcentaje >= 0.3f -> "⚠ Nivel medio"
                        else -> "⚠ Nivel crítico"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }

            Spacer(Modifier.width(16.dp))

            // Círculo de progreso
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { tanque.porcentaje },
                    modifier = Modifier.size(72.dp),
                    strokeWidth = 7.dp,
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "${(tanque.porcentaje * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// PESTAÑA 2: de recargas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecargasTab() {
    val context = LocalContext.current
    var proveedor by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var expandedTipo by remember { mutableStateOf(false) }
    var tipoSeleccionado by remember { mutableStateOf("CORRIENTE") }
    var estado by remember { mutableStateOf("") }
    val combustibles = listOf("CORRIENTE", "EXTRA", "DIESEL", "PREMIUM")

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Registrar Recarga", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = expandedTipo, onExpandedChange = { expandedTipo = !expandedTipo }) {
            OutlinedTextField(
                value = tipoSeleccionado, onValueChange = {}, readOnly = true,
                label = { Text("Tipo de combustible") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
            )
            ExposedDropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }) {
                combustibles.forEach { tipo ->
                    DropdownMenuItem(text = { Text(tipo) }, onClick = { tipoSeleccionado = tipo; expandedTipo = false })
                }
            }
        }

        OutlinedTextField(value = proveedor, onValueChange = { proveedor = it }, label = { Text("Proveedor") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = cantidad, onValueChange = { cantidad = it }, label = { Text("Cantidad (galones)") }, modifier = Modifier.fillMaxWidth())

        if (estado.isNotEmpty()) {
            Text(text = estado, color = if (estado.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                if (proveedor.isEmpty() || cantidad.isEmpty()) {
                    estado = "Completa todos los campos"
                    return@Button
                }
                // TODO: conectar endpoint de recarga
                estado = "✓ Recarga registrada correctamente"
                proveedor = ""; cantidad = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar Recarga")
        }
    }
}

// ── PESTAÑA 3: HISTORIAL ──

@Composable
fun HistorialTab() {
    // Datos de ejemplo mendiz conectaria con back
    val historial = listOf(
        RecargaUi("CORRIENTE", "Terpel S.A.", 3000, "2024-03-15"),
        RecargaUi("EXTRA", "Biomax", 2000, "2024-03-12"),
        RecargaUi("DIESEL", "Terpel S.A.", 4000, "2024-03-10"),
        RecargaUi("PREMIUM", "Primax", 1500, "2024-03-08"),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Historial de Movimientos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }
        items(historial) { recarga ->
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(recarga.tipo, fontWeight = FontWeight.Bold)
                        Text("Proveedor: ${recarga.proveedor}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Fecha: ${recarga.fecha}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("+${recarga.cantidad} gal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            // Botones exportar — TODO: conectar funcionalidad
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { /* TODO: exportar Excel */ }, modifier = Modifier.weight(1f)) {
                    Text("Exportar Excel")
                }
                OutlinedButton(onClick = { /* TODO: exportar PDF */ }, modifier = Modifier.weight(1f)) {
                    Text("Exportar PDF")
                }
            }
        }
    }
}
