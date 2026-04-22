package co.edu.unipiloto.fuelcontrol

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.FileProvider
import co.edu.unipiloto.fuelcontrol.api.IEmployeeApi
import co.edu.unipiloto.fuelcontrol.api.IStationApi
import co.edu.unipiloto.fuelcontrol.api.UpdateFuelPriceRequest
import co.edu.unipiloto.fuelcontrol.api.requests.ChangePasswordRequest
import co.edu.unipiloto.fuelcontrol.api.requests.CreateEmployeeRequest
import co.edu.unipiloto.fuelcontrol.api.requests.MeResponse
import co.edu.unipiloto.fuelcontrol.api.requests.UpdateEmployee
import co.edu.unipiloto.fuelcontrol.models.FuelPriceDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AdminDestinations(
    val label: String,
    val icon: ImageVector
) {
    INICIO("Inicio", Icons.Default.Home),
    USUARIOS("Usuarios", Icons.Default.ManageAccounts),
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

    var currentDestination by remember {
        mutableStateOf(AdminDestinations.INICIO)
    }
    var permissions by remember { mutableStateOf<List<String>>(emptyList()) }
    var role by remember { mutableStateOf<String>("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val api = Client.getClient(context).create(IAuthApi::class.java)

        api.getMe().enqueue(object : Callback<MeResponse> {

            override fun onResponse(
                call: Call<MeResponse>,
                response: Response<MeResponse>
            ) {
                if (response.isSuccessful) {
                    permissions = response.body()?.permissions ?: emptyList()
                    role = response.body()?.role ?: ""
                }
                loading = false
            }

            override fun onFailure(call: Call<MeResponse>, t: Throwable) {
                Log.e("AUTH", "Error", t)
                loading = false
            }
        })
    }

    val allowedDestinations = AdminDestinations.entries.filter { destination ->
        when (destination) {
            AdminDestinations.INICIO -> true

            AdminDestinations.PRECIOS ->
                permissions.contains("MANAGE_PRICES")

            AdminDestinations.INVENTARIO ->
                permissions.contains("MANAGE_INVENTORY")

            AdminDestinations.FACTURAS ->
                permissions.contains("MANAGE_FACTURATION")

            AdminDestinations.USUARIOS ->
                role == "STATION_ADMIN"

            AdminDestinations.PERFIL ->
                true

            AdminDestinations.ALERTAS ->
                true
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            allowedDestinations.forEach {
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
                AdminDestinations.INICIO ->
                    InicioScreen(modifier = Modifier.padding(innerPadding))
                AdminDestinations.USUARIOS ->
                    GestionUsuariosScreen(modifier = Modifier.padding(innerPadding))
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
fun InicioScreen(modifier: Modifier = Modifier) {

    val context = LocalContext.current
    val api = Client.getClient(context).create(IAuthApi::class.java)

    var user by remember { mutableStateOf<MeResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val api = Client.getClient(context).create(IAuthApi::class.java)

        api.getMe().enqueue(object : Callback<MeResponse> {

            override fun onResponse(
                call: Call<MeResponse>,
                response: Response<MeResponse>
            ) {
                if (response.isSuccessful) {
                    user = response.body()
                }
                loading = false
            }

            override fun onFailure(call: Call<MeResponse>, t: Throwable) {
                Log.e("AUTH", "Error", t)
                loading = false
            }
        })
    }

    when {
        loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        error != null -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        else -> user?.let {
            InicioContent(it, modifier)
        }
    }
}

@Composable
fun InicioContent(user: MeResponse, modifier: Modifier = Modifier) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Bienvenido, ${user.name}",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text("📧 ${user.email}")
                Text("👤 ${user.username}")
                Text("🏠 ${user.address ?: "No disponible"}")
                Text("🎂 ${user.birthDate ?: "No disponible"}")
                Text("⚧ ${user.gender ?: "No disponible"}")
                Text("🔐 Rol: ${user.role}")

                if (user.stationId != null) {
                    Text("⛽ Estación ID: ${user.stationId}")
                }
            }
        }

        if (!user.permissions.isNullOrEmpty()) {

            Text(
                text = "Permisos",
                style = MaterialTheme.typography.titleMedium
            )

            user.permissions.forEach {
                AssistChip(
                    onClick = {},
                    label = { Text(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionUsuariosScreen(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Crear", "Usuarios activos")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }
        when (selectedTab) {
            0 -> CrearUsuarioTab()
            1 -> UsuariosActivosTab()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearUsuarioTab() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = Client.getClient(context).create(IAuthApi::class.java)

    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var estado by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val allPermissions = listOf(
        "MANAGE_PRICES",
        "MANAGE_INVENTORY",
        "MANAGE_FACTURATION"
    )

    var selectedPermissions by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        item {
            Text(
                "Crear usuario con permisos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre completo") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Permisos",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            // 🔹 Lista de permisos con checkbox
            allPermissions.forEach { permission ->

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = selectedPermissions.contains(permission),
                        onCheckedChange = { checked ->
                            selectedPermissions =
                                if (checked) selectedPermissions + permission
                                else selectedPermissions - permission
                        }
                    )

                    Text(permission)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (estado.isNotEmpty()) {
                Text(
                    text = estado,
                    color = if (estado.startsWith("✓"))
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    if (nombre.isEmpty() || email.isEmpty() || password.isEmpty()) {
                        estado = "Completa todos los campos"
                        return@Button
                    }

                    scope.launch {
                        isLoading = true
                        try {

                            val request = CreateEmployeeRequest(
                                name = nombre,
                                email = email,
                                password = password,
                                permissions = selectedPermissions.toList()
                            )

                            api.createEmployee(request).enqueue(object : Callback<Void> {

                                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                    isLoading = false

                                    if (response.isSuccessful) {
                                        estado = "✓ Usuario creado"
                                    } else {
                                        estado = "Error: ${response.code()}"
                                    }
                                }

                                override fun onFailure(call: Call<Void>, t: Throwable) {
                                    isLoading = false
                                    estado = "Error: ${t.message}"
                                }
                            })

                            estado = "✓ Usuario creado"

                            nombre = ""
                            email = ""
                            password = ""
                            selectedPermissions = emptySet()

                        } catch (e: Exception) {
                            estado = "Error: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Crear usuario")
                }
            }
        }
    }
}

fun eliminarUsuario(
    api: IEmployeeApi,
    id: Long,
    onSuccess: () -> Unit
) {
    val scope = CoroutineScope(Dispatchers.IO)

    scope.launch {
        try {
            api.deleteEmployee(id)
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        } catch (e: Exception) {
            Log.e("DELETE", "Error", e)
        }
    }
}

@Composable
fun EditPermissionsDialog(
    usuario: UsuarioRolDto,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {

    val allPermissions = listOf(
        "MANAGE_PRICES",
        "MANAGE_INVENTORY",
        "MANAGE_FACTURATION"
    )

    var selected by remember {
        mutableStateOf(usuario.permissions.toSet())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar permisos") },
        text = {
            Column {
                allPermissions.forEach { perm ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selected.contains(perm),
                            onCheckedChange = { checked ->
                                selected =
                                    if (checked) selected + perm
                                    else selected - perm
                            }
                        )
                        Text(perm)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(selected.toList())
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun UsuariosActivosTab() {

    val context = LocalContext.current
    val api = Client.getClient(context).create(IEmployeeApi::class.java)

    var usuarios by remember { mutableStateOf<List<UsuarioRolDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var usuarioAEliminar by remember { mutableStateOf<UsuarioRolDto?>(null) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var usuarioAEditar by remember { mutableStateOf<UsuarioRolDto?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        try {
            usuarios = api.getEmployees()
        } catch (e: Exception) {
            error = "Error cargando usuarios"
        } finally {
            loading = false
        }
    }

    // 🔥 Mostrar snackbar
    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            mensaje = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        when {
            loading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            error != null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }

            usuarios.isEmpty() -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay usuarios")
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(usuarios) { usuario ->
                        UsuarioRolCard(
                            usuario = usuario,
                            onEliminar = {
                                usuarioAEliminar = usuario
                            },
                            onEditar = {
                               usuarioAEditar = usuario
                            }
                        )
                    }
                }
            }
        }
    }

    if (usuarioAEliminar != null) {
        AlertDialog(
            onDismissRequest = { usuarioAEliminar = null },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Seguro que deseas eliminar este usuario?") },
            confirmButton = {
                Button(onClick = {

                    eliminarUsuario(api, usuarioAEliminar!!.id) {
                        usuarios = usuarios.filter { it.id != usuarioAEliminar!!.id }
                        mensaje = "Usuario eliminado correctamente"
                    }

                    usuarioAEliminar = null
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { usuarioAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (usuarioAEditar != null) {
        EditPermissionsDialog(
            usuario = usuarioAEditar!!,
            onDismiss = { usuarioAEditar = null },
            onSave = { nuevosPermisos ->

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        println(usuarioAEditar!!.toString())
                        api.updatePermissions(
                            usuarioAEditar!!.id,
                            UpdateEmployee(
                                name = usuarioAEditar!!.name,
                                permissions = nuevosPermisos
                            )
                        )

                        withContext(Dispatchers.Main) {
                            usuarios = usuarios.map {
                                if (it.id == usuarioAEditar!!.id) {
                                    it.copy(permissions = nuevosPermisos)
                                } else it
                            }

                            mensaje = "Permisos actualizados"
                            usuarioAEditar = null
                        }

                    } catch (e: Exception) {
                        Log.e("EDIT", "Error", e)
                    }
                }
            }
        )
    }
}
@Composable
fun UsuarioRolCard(
    usuario: UsuarioRolDto,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f) // 🔥 clave
                ) {
                    Text(
                        usuario.name,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        usuario.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, // 🔥 evita el "p e p e"
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.widthIn(max = 160.dp) // 🔥 limita ancho
                ) {
                    Text(
                        text = usuario.permissions.joinToString(", "),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2, // 🔥 no crece infinito
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                OutlinedButton(
                    onClick = onEditar,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Editar")
                }

                Button(
                    onClick = onEliminar,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            }
        }
    }
}

data class UsuarioRolDto(val id: Long, val name: String, val email: String, val permissions: List<String>)


@Composable
fun PerfilAdminScreen(modifier: Modifier = Modifier) {

    val context = LocalContext.current

    var user by remember { mutableStateOf<MeResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var mensaje by remember { mutableStateOf("") }

    var passwordActual by remember { mutableStateOf("") }
    var nuevaPassword by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Cambiar contraseña",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = passwordActual,
            onValueChange = { passwordActual = it },
            label = { Text("Contraseña actual") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = nuevaPassword,
            onValueChange = { nuevaPassword = it },
            label = { Text("Nueva contraseña") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = confirmarPassword,
            onValueChange = { confirmarPassword = it },
            label = { Text("Confirmar contraseña") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (mensaje.isNotEmpty()) {
            Text(
                text = mensaje,
                color = if (mensaje.startsWith("✓"))
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {

                if (passwordActual.isEmpty()) {
                    mensaje = "Ingresa la contraseña actual"
                    return@Button
                }

                if (nuevaPassword != confirmarPassword) {
                    mensaje = "Las contraseñas no coinciden"
                    return@Button
                }

                Thread {
                    try {
                        val api = Client.getClient(context).create(IAuthApi::class.java)

                        val response = api.changePassword(
                            ChangePasswordRequest(
                                passwordActual,
                                nuevaPassword
                            )
                        ).execute()

                        if (response.isSuccessful) {
                            mensaje = "✓ Contraseña actualizada"

                            passwordActual = ""
                            nuevaPassword = ""
                            confirmarPassword = ""

                        } else {
                            mensaje = "Error al actualizar contraseña"
                        }

                    } catch (e: Exception) {
                        mensaje = "Error de conexión"
                    }
                }.start()
            }
        ) {
            Text("Cambiar contraseña")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            onClick = {

                val prefs = context.getSharedPreferences("FuelControlPrefs", Context.MODE_PRIVATE)
                prefs.edit {clear()}

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
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Registrar Recarga", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = expandedTipo, onExpandedChange = { expandedTipo = !expandedTipo }) {
            OutlinedTextField(
                value = tipoSeleccionado, onValueChange = {}, readOnly = true,
                label = { Text("Tipo de combustible") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Historial de Movimientos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
            items(historial) { mov ->
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
            fun generarPDF(context: Context, historial: List<InventoryMovementResponse>) {
                val document = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint()

                // Título
                paint.textSize = 20f
                paint.isFakeBoldText = true
                canvas.drawText("Historial de Movimientos de Inventario", 40f, 60f, paint)

                // Fecha de generación (no se si funciona)
                paint.textSize = 12f
                paint.isFakeBoldText = false
                paint.color = Color.GRAY
                canvas.drawText("Generado: ${
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(
                        Date()
                    )}", 40f, 85f, paint)

                // Línea separadora
                paint.color = Color.BLACK
                paint.strokeWidth = 1f
                canvas.drawLine(40f, 95f, 555f, 95f, paint)

                // Encabezados
                paint.isFakeBoldText = true
                paint.textSize = 13f
                canvas.drawText("Tipo", 40f, 120f, paint)
                canvas.drawText("Proveedor", 160f, 120f, paint)
                canvas.drawText("Cantidad", 340f, 120f, paint)
                canvas.drawText("Fecha", 440f, 120f, paint)
                canvas.drawLine(40f, 128f, 555f, 128f, paint)

                // deberian verse las filas
                paint.isFakeBoldText = false
                paint.textSize = 12f
                var y = 150f
                historial.forEach { mov ->
                    canvas.drawText(mov.fuelType, 40f, y, paint)
                    canvas.drawText(mov.supplier ?: "Venta", 160f, y, paint)
                    canvas.drawText("${mov.gallonsAdded.toInt()} gal", 340f, y, paint)
                    canvas.drawText(mov.rechargeDate.take(10), 440f, y, paint)
                    y += 30f
                }

                canvas.drawLine(40f, y + 5f, 555f, y + 5f, paint)
                document.finishPage(page)

                try {
                    val fileName = "inventario_${System.currentTimeMillis()}.pdf"
                    val file = File(context.getExternalFilesDir(null), fileName)
                    document.writeTo(FileOutputStream(file))
                    document.close()

                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                    Toast.makeText(context, "PDF generado correctamente", Toast.LENGTH_SHORT).show()

                } catch (e: Exception) {
                    Toast.makeText(context, "Error al generar PDF", Toast.LENGTH_SHORT).show()
                }
            }

           //deberia pedir la app de pdf del celu
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { generarPDF(context, historial) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Exportar PDF")
                }
            }
        }
    }

}