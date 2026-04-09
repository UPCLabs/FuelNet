package co.edu.unipiloto.fuelcontrol

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import co.edu.unipiloto.fuelcontrol.api.Client
import co.edu.unipiloto.fuelcontrol.api.IAuthApi
import co.edu.unipiloto.fuelcontrol.api.requests.CreatePaymentRequest
import co.edu.unipiloto.fuelcontrol.models.PaymentSummaryResponse
import co.edu.unipiloto.fuelcontrol.ui.theme.FuelControlTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.core.content.edit
import co.edu.unipiloto.fuelcontrol.api.IInventoryApi
import co.edu.unipiloto.fuelcontrol.api.IPaymentApi
import co.edu.unipiloto.fuelcontrol.api.requests.FuelTankResponse
import co.edu.unipiloto.fuelcontrol.api.requests.InventoryMovementResponse
import co.edu.unipiloto.fuelcontrol.api.requests.PaymentResponse
import co.edu.unipiloto.fuelcontrol.api.requests.RechargeRequest
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.HorizontalDivider
import co.edu.unipiloto.fuelcontrol.api.IStationApi
import co.edu.unipiloto.fuelcontrol.api.UpdateFuelPriceRequest
import co.edu.unipiloto.fuelcontrol.models.FuelPriceDto
import kotlinx.coroutines.launch

enum class AdminDestinations(
    val label: String,
    val icon: ImageVector
) {
    FACTURAS("Facturas", Icons.Default.Description),
    INVENTARIO("Inventario", Icons.Default.Inventory),

    ALERTAS("Alertas", Icons.Default.Notifications),

    PRECIOS("Precios", Icons.Default.AttachMoney),

    PERFIL("Perfil", Icons.Default.AccountBox)
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
                AdminDestinations.ALERTAS -> NotificacionesScreen(modifier = Modifier.padding(innerPadding))
                AdminDestinations.PRECIOS -> PreciosScreen(modifier = Modifier.padding(innerPadding))
                AdminDestinations.PERFIL -> PerfilAdminScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

@Composable
fun PerfilAdminScreen(modifier: Modifier = Modifier) {

    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Panel Administrador",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            onClick = {

                val prefs = context.getSharedPreferences("FuelControlPrefs", Context.MODE_PRIVATE)
                prefs.edit { clear() }

                val intent = Intent(context, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            }
        ) {
            Text("Cerrar sesión")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacturasScreen(modifier: Modifier = Modifier) {

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("FuelControlPrefs", Context.MODE_PRIVATE)
    val token = "Bearer " + (prefs.getString("token", "") ?: "")
    val api = Client.getClient(context).create(IPaymentApi::class.java)

    var email by remember { mutableStateOf("") }
    var galones by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var combustibleSeleccionado by remember { mutableStateOf("CORRIENTE") }
    var expandedSpinner by remember { mutableStateOf(false) }
    var tvEstado by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var facturas by remember { mutableStateOf<List<PaymentResponse>>(emptyList()) }

    val combustibles = listOf("CORRIENTE", "EXTRA", "DIESEL")

    // Cargar facturas del admin al entrar
    LaunchedEffect(Unit) {
        api.getAdminPayments(token).enqueue(object : Callback<List<PaymentResponse>> {
            override fun onResponse(call: Call<List<PaymentResponse>>, response: Response<List<PaymentResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    facturas = response.body()!!
                }
            }
            override fun onFailure(call: Call<List<PaymentResponse>>, t: Throwable) {
                Toast.makeText(context, "Error cargando facturas", Toast.LENGTH_SHORT).show()
            }
        })
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Panel Administrador",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo del cliente") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // Dropdown tipo combustible
            ExposedDropdownMenuBox(
                expanded = expandedSpinner,
                onExpandedChange = { expandedSpinner = !expandedSpinner }
            ) {
                OutlinedTextField(
                    value = combustibleSeleccionado,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de combustible") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSpinner) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                )
                ExposedDropdownMenu(
                    expanded = expandedSpinner,
                    onDismissRequest = { expandedSpinner = false }
                ) {
                    combustibles.forEach { tipo ->
                        DropdownMenuItem(
                            text = { Text(tipo) },
                            onClick = { combustibleSeleccionado = tipo; expandedSpinner = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = galones,
                onValueChange = { galones = it },
                label = { Text("Galones") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = monto,
                onValueChange = { monto = it },
                label = { Text("Monto ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            if (tvEstado.isNotEmpty()) {
                Text(
                    text = tvEstado,
                    color = if (tvEstado.startsWith("✓"))
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (email.isEmpty() || galones.isEmpty() || monto.isEmpty()) {
                        tvEstado = "Completa todos los campos"
                        return@Button
                    }
                    isLoading = true
                    val request = CreatePaymentRequest(
                        userEmail = email,
                        fuelType = combustibleSeleccionado,
                        gallons = galones.toDoubleOrNull() ?: 0.0,
                        amount = monto.toDoubleOrNull() ?: 0.0
                    )
                    api.createPayment(token, request).enqueue(object : Callback<PaymentResponse> {
                        override fun onResponse(call: Call<PaymentResponse>, response: Response<PaymentResponse>) {
                            isLoading = false
                            if (response.isSuccessful && response.body() != null) {
                                tvEstado = "✓ Factura creada correctamente"
                                facturas = listOf(response.body()!!) + facturas
                                email = ""; galones = ""; monto = ""
                            } else {
                                tvEstado = "Error al crear la factura (${response.code()})"
                            }
                        }
                        override fun onFailure(call: Call<PaymentResponse>, t: Throwable) {
                            isLoading = false
                            tvEstado = "Error de conexión"
                        }
                    })
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Crear Factura")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Facturas registradas",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
        }

        items(facturas) { factura ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            factura.clientName,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            factura.clientEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${factura.fuelType} • ${factura.gallons} gal • $${factura.amount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = factura.status,
                        color = if (factura.status == "PENDING")
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreciosScreen(modifier: Modifier = Modifier) {

    val context = LocalContext.current
    val api = Client.getClient(context).create(IStationApi::class.java)
    val scope = rememberCoroutineScope()

    var precio by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("CORRIENTE") }
    var expanded by remember { mutableStateOf(false) }
    var estado by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    var preciosActuales by remember { mutableStateOf<List<FuelPriceDto>>(emptyList()) }
    var loadingPrecios by remember { mutableStateOf(true) }

    val combustibles = listOf("CORRIENTE", "EXTRA", "DIESEL")

    LaunchedEffect(Unit) {
        try {
            preciosActuales = api.getMyPrices()
        } catch (e: Exception) {
           Toast.makeText(context, "Error cargando precios", Toast.LENGTH_SHORT).show()
        }
        loadingPrecios = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text("Actualizar precios", style = MaterialTheme.typography.titleLarge)

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            val fillMaxWidth = Modifier.fillMaxWidth()
            OutlinedTextField(
                value = tipo,
                onValueChange = {},
                readOnly = true,
                label = { Text("Tipo de combustible") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                },
                modifier = fillMaxWidth.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)

            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                combustibles.forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            tipo = it
                            expanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = precio,
            onValueChange = { precio = it },
            label = { Text("Nuevo precio") },
            modifier = Modifier.fillMaxWidth()
        )

        if (estado.isNotEmpty()) {
            Text(
                estado,
                color = if (estado.startsWith("✓"))
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                val precioDouble = precio.toDoubleOrNull()

                if (precioDouble == null) {
                    estado = "Precio inválido"
                    return@Button
                }

                scope.launch {
                    loading = true
                    try {
                        val request = listOf(UpdateFuelPriceRequest(tipo, precioDouble))
                        api.updatePrices(request)

                        estado = "✓ Precio actualizado"
                        precio = ""

                        preciosActuales = api.getMyPrices()

                    } catch (e: Exception) {
                        Log.e("Admin", "Error actualizando precio", e)
                        estado = "✗ Error: ${e.message}"
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else {
                Text("Actualizar precio")
            }
        }
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        Text("Precios actuales", style = MaterialTheme.typography.titleMedium)

        if (loadingPrecios) {
            CircularProgressIndicator()
        } else {
            preciosActuales.forEach {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(it.fuelType ?: "—")
                        Text(it.price?.let { p -> "$$p" } ?: "—")
                    }
                }
            }
        }
    }
}

@Composable
fun NivelesTab() {
    val context = LocalContext.current
    val token = "Bearer " + (context.getSharedPreferences("FuelControlPrefs", Context.MODE_PRIVATE)
        .getString("token", "") ?: "")
    val api = Client.getClient(context).create(IInventoryApi::class.java)

    var tanques by remember { mutableStateOf<List<FuelTankResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        api.getDashboard(token).enqueue(object : Callback<List<FuelTankResponse>> {
            override fun onResponse(call: Call<List<FuelTankResponse>>, response: Response<List<FuelTankResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    tanques = response.body()!!
                    tanques.filter { it.fillPercentage <= 15.0 }.forEach {

                    }
                } else {
                    error = "Error al cargar niveles (${response.code()})"
                }
                isLoading = false
            }
            override fun onFailure(call: Call<List<FuelTankResponse>>, t: Throwable) {
                error = "Error de conexión"
                isLoading = false
            }
        })
    }

    when {
        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tanques) { tanque ->
                TanqueCard(tanque)
            }
        }
    }
}

@Composable
fun TanqueCard(tanque: FuelTankResponse) {
    val porcentaje = (tanque.fillPercentage / 100).toFloat()
    val color = when {
        porcentaje >= 0.6f -> MaterialTheme.colorScheme.primary
        porcentaje >= 0.3f -> MaterialTheme.colorScheme.tertiary
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
                Text(tanque.fuelType, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${tanque.currentLevelGallons.toInt()} / ${tanque.capacityGallons.toInt()} gal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        porcentaje >= 0.6f -> "✓ Nivel normal"
                        porcentaje >= 0.3f -> "⚠ Nivel medio"
                        else -> "⚠ Nivel crítico"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
            Spacer(Modifier.width(16.dp))
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { porcentaje },
                    modifier = Modifier.size(72.dp),
                    strokeWidth = 7.dp,
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "${tanque.fillPercentage.toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── PESTAÑA 2: RECARGAS ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecargasTab() {
    val context = LocalContext.current
    val token = "Bearer " + (context.getSharedPreferences("FuelControlPrefs", Context.MODE_PRIVATE)
        .getString("token", "") ?: "")
    val api = Client.getClient(context).create(IInventoryApi::class.java)

    var proveedor by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }
    var expandedTipo by remember { mutableStateOf(false) }
    var tipoSeleccionado by remember { mutableStateOf("CORRIENTE") }
    var estado by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val combustibles = listOf("CORRIENTE", "EXTRA", "DIESEL")

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

        OutlinedTextField(
            value = proveedor, onValueChange = { proveedor = it },
            label = { Text("Proveedor") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = cantidad, onValueChange = { cantidad = it },
            label = { Text("Cantidad (galones)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = notas, onValueChange = { notas = it },
            label = { Text("Notas (opcional)") },
            modifier = Modifier.fillMaxWidth()
        )

        if (estado.isNotEmpty()) {
            Text(
                text = estado,
                color = if (estado.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                if (proveedor.isEmpty() || cantidad.isEmpty()) {
                    estado = "Completa todos los campos"
                    return@Button
                }
                isLoading = true
                val request = RechargeRequest(
                    fuelType = tipoSeleccionado,
                    gallonsAdded = cantidad.toDoubleOrNull() ?: 0.0,
                    supplier = proveedor,
                    notes = notas.ifEmpty { null }
                )
                api.recharge(token, request).enqueue(object : Callback<InventoryMovementResponse> {
                    override fun onResponse(call: Call<InventoryMovementResponse>, response: Response<InventoryMovementResponse>) {
                        isLoading = false
                        if (response.isSuccessful) {
                            estado = "✓ Recarga registrada correctamente"
                            proveedor = ""; cantidad = ""; notas = ""
                        } else {
                            estado = "Error al registrar recarga (${response.code()})"
                        }
                    }
                    override fun onFailure(call: Call<InventoryMovementResponse>, t: Throwable) {
                        isLoading = false
                        estado = "Error de conexión"
                    }
                })
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Registrar Recarga")
            }
        }
    }
}

@Composable
fun HistorialTab() {
    val context = LocalContext.current
    val token = "Bearer " + (context.getSharedPreferences("FuelControlPrefs", Context.MODE_PRIVATE)
        .getString("token", "") ?: "")
    val api = Client.getClient(context).create(IInventoryApi::class.java)

    var historial by remember { mutableStateOf<List<InventoryMovementResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        api.getHistory(token).enqueue(object : Callback<List<InventoryMovementResponse>> {
            override fun onResponse(call: Call<List<InventoryMovementResponse>>, response: Response<List<InventoryMovementResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    historial = response.body()!!
                } else {
                    error = "Error al cargar historial (${response.code()})"
                }
                isLoading = false
            }
            override fun onFailure(call: Call<List<InventoryMovementResponse>>, t: Throwable) {
                error = "Error de conexión"
                isLoading = false
            }
        })
    }

    when {
        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Historial de Movimientos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
            items(historial) { mov ->
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mov.fuelType, fontWeight = FontWeight.Bold)
                            Text(
                                "Proveedor: ${mov.supplier ?: "Venta"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Fecha: ${mov.rechargeDate.take(10)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (mov.notes != null) {
                                Text(
                                    mov.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = if (mov.gallonsAdded >= 0) "+${mov.gallonsAdded.toInt()} gal"
                            else "${mov.gallonsAdded.toInt()} gal",
                            fontWeight = FontWeight.Bold,
                            color = if (mov.gallonsAdded >= 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}