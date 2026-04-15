package co.edu.unipiloto.fuelcontrol

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import co.edu.unipiloto.fuelcontrol.api.AdminAceptationRequest
import co.edu.unipiloto.fuelcontrol.api.Client
import co.edu.unipiloto.fuelcontrol.ui.theme.FuelControlTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import co.edu.unipiloto.fuelcontrol.api.IPendingUserApi


enum class SuperAdminDestinations(
    val label: String,
    val icon: ImageVector
) {
    HOME("Inicio", Icons.Default.Home),
    SOLICITUDES("Solicitudes", Icons.Default.PersonAdd),
    PERFIL("Perfil", Icons.Default.AccountBox)
}


class SuperAdminDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FuelControlTheme {
                SuperAdminDashboardScreen()
            }
        }
    }
}


@Composable
fun SuperAdminDashboardScreen() {
    var currentDestination by remember { mutableStateOf(SuperAdminDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            SuperAdminDestinations.entries.forEach {
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
                SuperAdminDestinations.HOME ->
                    SuperAdminHomeScreen(modifier = Modifier.padding(innerPadding))
                SuperAdminDestinations.SOLICITUDES ->
                    SolicitudesScreen(modifier = Modifier.padding(innerPadding))
                SuperAdminDestinations.PERFIL ->
                    SuperAdminPerfilScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}


@Composable
fun SuperAdminHomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("FuelControlPrefs", Context.MODE_PRIVATE)
    val adminName = prefs.getString("userName", "Administrador") ?: "Administrador"

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "👋 Bienvenido,",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = adminName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Desde aquí puedes revisar y gestionar las solicitudes de registro de nuevos usuarios en la plataforma FuelControl.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}


@Composable
fun SolicitudesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("FuelControlPrefs", Context.MODE_PRIVATE)
    val token = "Bearer " + (prefs.getString("token", "") ?: "")
    val api = Client.getClient(context).create(IPendingUserApi::class.java)

    var solicitudes by remember { mutableStateOf<List<PendingUserDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedUser by remember { mutableStateOf<PendingUserDto?>(null) }
    var resolvingId by remember { mutableStateOf<Long?>(null) }

    fun loadSolicitudes() {
        isLoading = true
        error = null
        api.getPendingUsers().enqueue(object : Callback<List<PendingUserDto>> {
            override fun onResponse(
                call: Call<List<PendingUserDto>>,
                response: Response<List<PendingUserDto>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    solicitudes = response.body()!!
                } else {
                    error = "Error al cargar solicitudes (${response.code()})"
                }
                isLoading = false
            }
            override fun onFailure(call: Call<List<PendingUserDto>>, t: Throwable) {
                error = "Error de conexión"
                isLoading = false
            }
        })
    }

    fun resolve(user: PendingUserDto, accepted: Boolean) {
        resolvingId = user.id
        val request = AdminAceptationRequest(
            pendingUserId = user.id,
            accepted = accepted,
            roleRequested = user.roleRequested
        )
        api.resolveUser(request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                resolvingId = null
                selectedUser = null
                loadSolicitudes()
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                resolvingId = null
                selectedUser = null
            }
        })
    }

    LaunchedEffect(Unit) { loadSolicitudes() }

    selectedUser?.let { user ->
        AlertDialog(
            onDismissRequest = { selectedUser = null },
            title = {
                Text("Detalle de solicitud", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DetalleRow("Nombre", user.name)
                    DetalleRow("Email", user.email)
                    DetalleRow("Dirección", user.address)
                    DetalleRow("Fecha de nac.", user.birthDate.take(10))
                    DetalleRow("Género", user.gender)
                    DetalleRow("Rol solicitado", user.roleRequested)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        resolve(user, true)
                    }
                ) {
                    Text("✓ Aprobar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        resolve(user, false)
                    }
                ) {
                    Text("✗ Rechazar")
                }
            }
        )
    }

    when {
        isLoading -> Box(Modifier.fillMaxSize().then(modifier), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        error != null -> Box(Modifier.fillMaxSize().then(modifier), contentAlignment = Alignment.Center) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        solicitudes.isEmpty() -> Box(
            Modifier.fillMaxSize().then(modifier),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "No hay solicitudes pendientes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        else -> LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Solicitudes pendientes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${solicitudes.size} solicitud(es) por revisar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }

            items(solicitudes) { user ->
                SolicitudCard(
                    user = user,
                    isResolving = resolvingId == user.id,
                    onVerDetalle = { selectedUser = user },
                    onAprobar = {
                        resolve(user, true)
                    },
                    onRechazar = {
                        resolve(user, false)
                    }
                )
            }
        }
    }
}


@Composable
fun SolicitudCard(
    user: PendingUserDto,
    isResolving: Boolean,
    onVerDetalle: () -> Unit,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isResolving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = user.roleRequested,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (isResolving) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Procesando...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onVerDetalle,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ver detalle", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onRechazar,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Rechazar", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = onAprobar,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Aprobar", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}


@Composable
fun DetalleRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.6f)
        )
    }
}


@Composable
fun SuperAdminPerfilScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("FuelControlPrefs", Context.MODE_PRIVATE)
    val adminName = prefs.getString("userName", "Administrador") ?: "Administrador"
    val adminEmail = prefs.getString("userEmail", "") ?: ""

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(88.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = adminName.firstOrNull()?.uppercase() ?: "A",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(adminName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        if (adminEmail.isNotEmpty()) {
            Text(
                adminEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp))

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = "Super Administrador",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Spacer(Modifier.height(40.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            onClick = {
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


data class PendingUserDto(
    val id: Long,
    val name: String,
    val email: String,
    val address: String,
    val birthDate: String,
    val gender: String,
    val roleRequested: String
)