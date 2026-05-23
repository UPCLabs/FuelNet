package co.edu.unipiloto.fuelcontrol

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import co.edu.unipiloto.fuelcontrol.api.Client
import co.edu.unipiloto.fuelcontrol.api.IAuthApi
import co.edu.unipiloto.fuelcontrol.api.IPaymentApi
import co.edu.unipiloto.fuelcontrol.api.IStationApi
import co.edu.unipiloto.fuelcontrol.api.requests.ChangePasswordRequest
import co.edu.unipiloto.fuelcontrol.api.requests.MeResponse
import co.edu.unipiloto.fuelcontrol.api.requests.PaymentResponse
import co.edu.unipiloto.fuelcontrol.models.AlertResponse
import co.edu.unipiloto.fuelcontrol.services.MapTimerService
import co.edu.unipiloto.fuelcontrol.services.SmartRouteService
import co.edu.unipiloto.fuelcontrol.ui.theme.FuelControlTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.annotations.SerializedName
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Gasolinera(
    val id: Long,
    @SerializedName("name")
    val nombre: String,
    @SerializedName("address")
    val direccion: String,
    val latLng: LatLng?,
    var precio: String = "Cargando..."
)
data class LocalPriceAlert(
    val stationName: String,
    val message: String,
    val date: String
)
class DashboardActivity : ComponentActivity() {

    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        enableEdgeToEdge()
        setContent {
            FuelControlTheme {
                FuelControlApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelControlApp() {

    var currentDestination by remember {
        mutableStateOf(AppDestinations.HOME)
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {

        val context = LocalContext.current

        val apiService = remember {
            Client.getClient(context).create(IStationApi::class.java)
        }

        Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = currentDestination.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    when (currentDestination) {
                        AppDestinations.MAPA -> {
                            IconButton(onClick = { /* futuro: filtro de combustible */ }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filtrar"
                                )
                            }
                        }
                        AppDestinations.PAGOS -> {
                            IconButton(onClick = { /* futuro: historial */ }) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = "Historial"
                                )
                            }
                        }
                        else -> {}
                    }
                }
            )
        }) { innerPadding ->

            when (currentDestination) {

                AppDestinations.HOME -> {
                    HomeScreen(
                        onConsultarPrecios = {
                            currentDestination = AppDestinations.MAPA
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                AppDestinations.MAPA -> {
                    MapScreen(Modifier.padding(innerPadding), apiService)
                }

                AppDestinations.PAGOS -> {
                    PagosScreen(modifier = Modifier.padding(innerPadding))
                }

                AppDestinations.PERFIL -> {
                    PerfilScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                AppDestinations.NOTIFICACIONES -> {
//                    NotificacionesScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun PerfilScreen(modifier: Modifier = Modifier) {

    val context = LocalContext.current

    var user by remember { mutableStateOf<MeResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var mensaje by remember { mutableStateOf("") }

    var passwordActual by remember { mutableStateOf("") }
    var nuevaPassword by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {

        val api = Client.getClient(context).create(IAuthApi::class.java)

        api.getMe().enqueue(object : retrofit2.Callback<MeResponse> {

            override fun onResponse(
                call: retrofit2.Call<MeResponse>,
                response: retrofit2.Response<MeResponse>
            ) {
                if (response.isSuccessful) {
                    user = response.body()
                } else {
                    mensaje = "Error: ${response.code()}"
                }
                loading = false
            }

            override fun onFailure(call: retrofit2.Call<MeResponse>, t: Throwable) {
                Log.e("PERFIL_ERROR", "Fallo", t)
                mensaje = "Error de conexión"
                loading = false
            }
        })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {

        Text(
            text = "Perfil",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (loading) {
            CircularProgressIndicator()
        } else {
            user?.let {

                Text("Nombre: ${it.name}")
                Text("Email: ${it.email}")
                Text("Usuario: ${it.username}")
                Text("Dirección: ${it.address ?: "No disponible"}")
                Text("Fecha nacimiento: ${it.birthDate ?: "No disponible"}")
                Text("Género: ${it.gender ?: "No disponible"}")
                Text("Rol: ${it.role}")

                if (it.stationId != null) {
                    Text("Estación ID: ${it.stationId}")
                }
            }
        }

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
@Composable
fun HomeScreen(
    onConsultarPrecios: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "FuelNet",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onConsultarPrecios
        ) {
            Text("Consultar precios")
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Inicio", Icons.Default.Home),
    MAPA("Mapa", Icons.Default.Map),
    PAGOS("Pagos", Icons.Default.Payment),

    NOTIFICACIONES("Alertas", Icons.Default.Notifications),
    PERFIL("Perfil", Icons.Default.AccountBox),
}

suspend fun getLatLngFromAddress(context: Context, address: String): LatLng? {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val results = geocoder.getFromLocationName(address, 1)

            if (!results.isNullOrEmpty()) {
                val location = results[0]
                LatLng(location.latitude, location.longitude)
            } else null

        } catch (e: IOException) {
            null
        }
    }
}

@Composable
fun MapScreen(modifier: Modifier = Modifier, apiService: IStationApi) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val prefs = context.getSharedPreferences("FuelControlPrefs", Context.MODE_PRIVATE)

    fun getFollowedStations(): MutableSet<String> {
        return prefs.getStringSet("followed_stations", mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()
    }

    fun isFollowing(stationId: Long): Boolean {
        return getFollowedStations().contains(stationId.toString())
    }

    fun followStation(stationId: Long) {
        val set = getFollowedStations()
        set.add(stationId.toString())
        prefs.edit { putStringSet("followed_stations", set) }
    }

    fun unfollowStation(stationId: Long) {
        val set = getFollowedStations()
        set.remove(stationId.toString())
        prefs.edit { putStringSet("followed_stations", set) }
    }

    var refreshFollowState by remember { mutableStateOf(0) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var gasolineras by remember { mutableStateOf<List<Gasolinera>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var gasolineraSeleccionada by remember { mutableStateOf<Gasolinera?>(null) }

    val bogota = LatLng(4.7110, -74.0721)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(bogota, 12f)
    }

    var segundosEnMapa by remember { mutableStateOf(0) }
    var timerService by remember { mutableStateOf<MapTimerService?>(null) }

    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                timerService = (binder as MapTimerService.LocalBinder).getService()
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                timerService = null
            }
        }
    }

    DisposableEffect(Unit) {
        val intent = Intent(context, MapTimerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        onDispose {
            context.unbindService(serviceConnection)
        }
    }

    LaunchedEffect(timerService) {
        while (timerService != null) {
            segundosEnMapa = timerService?.getSegundos() ?: 0
            delay(1000L)
        }
    }


    fun distanciaKm(a: LatLng, b: LatLng): Double {
        val R = 6371.0
        val dLat = Math.toRadians(b.latitude  - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val sinLat = sin(dLat / 2)
        val sinLng = sin(dLng / 2)
        val c = 2 * asin(
            sqrt(
                sinLat * sinLat +
                        cos(Math.toRadians(a.latitude)) *
                        cos(Math.toRadians(b.latitude)) *
                        sinLng * sinLng
            )
        )
        return R * c
    }

    LaunchedEffect(Unit) {

        try {
            val stations = apiService.getAllStations()
            gasolineras = stations.map { station ->
                Gasolinera(
                    id        = station.id,
                    nombre    = station.name,
                    direccion = station.address,
                    latLng    = getLatLngFromAddress(context, station.address)
                )
            }
            isLoading = false
        } catch (e: Exception) {
            Toast.makeText(context, "Error al cargar estaciones", Toast.LENGTH_LONG).show()
            isLoading = false
        }

        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    userLocation = userLatLng
                    cameraPositionState.position =
                        CameraPosition.fromLatLngZoom(userLatLng, 14f)

                    val conCoords = gasolineras.filter { g -> g.latLng != null }

                    if (conCoords.isNotEmpty()) {
                        val intent = Intent(context, SmartRouteService::class.java).apply {
                            putExtra("user_lat",      it.latitude)
                            putExtra("user_lng",      it.longitude)
                            putExtra("station_names", conCoords.map { g -> g.nombre }.toTypedArray())
                            putExtra("station_lats",  conCoords.map { g -> g.latLng!!.latitude }.toDoubleArray())
                            putExtra("station_lngs",  conCoords.map { g -> g.latLng!!.longitude }.toDoubleArray())
                        }
                        context.startService(intent)
                    }
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {

            userLocation?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Tu ubicación"
                )
            }

            gasolineras.forEach { gasolinera ->

                gasolinera.latLng?.let { position ->

                    Marker(
                        state = MarkerState(position = position),
                        title = gasolinera.nombre,
                        snippet = gasolinera.direccion,
                        onClick = {

                            gasolineraSeleccionada = gasolinera

                            scope.launch {

                                try {
                                    val response = apiService.getStationPrices(gasolinera.id)

                                    val precioFormateado =
                                        response.fuels.joinToString("\n") {
                                            "${it.fuelType}: $${it.price}"
                                        }

                                    gasolineraSeleccionada =
                                        gasolinera.copy(precio = precioFormateado)

                                } catch (e: Exception) {
                                    gasolineraSeleccionada =
                                        gasolinera.copy(precio = "Error al cargar precios")
                                }
                            }

                            cameraPositionState.position =
                                CameraPosition.fromLatLngZoom(position, 15f)

                            true
                        }
                    )
                }
            }
        }

        gasolineraSeleccionada?.let { seleccionada ->
            val siguiendo = isFollowing(seleccionada.id)
            refreshFollowState

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                Column(modifier = Modifier.padding(16.dp)) {

                    Text(
                        text = seleccionada.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(text = seleccionada.direccion)

                    Text(
                        text = seleccionada.precio,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {

                            val topic = "station_${seleccionada.id}"

                            if (!siguiendo) {

                                FirebaseMessaging.getInstance()
                                    .subscribeToTopic(topic)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {

                                            followStation(seleccionada.id)
                                            refreshFollowState++

                                            Toast.makeText(
                                                context,
                                                "Siguiendo ${seleccionada.nombre}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Error al seguir",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }

                            } else {

                                FirebaseMessaging.getInstance()
                                    .unsubscribeFromTopic(topic)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {

                                            unfollowStation(seleccionada.id)
                                            refreshFollowState++

                                            Toast.makeText(
                                                context,
                                                "Dejaste de seguir",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Error al dejar de seguir",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            }
                        }
                    ) {
                        Text(if (siguiendo) "Dejar de seguir" else "Seguir estación")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val lat = seleccionada.latLng?.latitude ?: return@Button
                            val lng = seleccionada.latLng.longitude

                            val wazeUri =
                                "https://waze.com/ul?ll=$lat,$lng&navigate=yes".toUri()

                            val intent = Intent(Intent.ACTION_VIEW, wazeUri)
                            context.startActivity(intent)
                        }
                    ) {
                        Text("Navegar en Waze")
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {

                            val lat = seleccionada.latLng?.latitude ?: return@Button
                            val lng = seleccionada.latLng.longitude

                            val gmmIntentUri =
                                "google.navigation:q=$lat,$lng".toUri()

                            val mapIntent =
                                Intent(Intent.ACTION_VIEW, gmmIntentUri)

                            mapIntent.setPackage("com.google.android.apps.maps")

                            context.startActivity(mapIntent)
                        }
                    ) {
                        Text("Navegar en Google Maps")
                    }


                }
            }
        }

        val minutos = segundosEnMapa / 60
        val segs    = segundosEnMapa % 60

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp
        ) {
            Text(
                text = "⏱ %02d:%02d en el mapa".format(minutos, segs),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun PagosScreen(modifier: Modifier = Modifier) {

    val context = LocalContext.current
    var pagos by remember { mutableStateOf<List<PaymentResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val prefs = context.getSharedPreferences("FuelControlPrefs", Context.MODE_PRIVATE)
    val token = prefs.getString("token", "") ?: ""
    val apiService = Client.getClient(context).create(IPaymentApi::class.java)

    fun loadPayments() {
        isLoading = true
        apiService.getMyPayments("Bearer $token").enqueue(object : Callback<List<PaymentResponse>> {
            override fun onResponse(call: Call<List<PaymentResponse>>, response: Response<List<PaymentResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    pagos = response.body()!!
                } else {
                    error = "Error al cargar pagos"
                }
                isLoading = false
            }
            override fun onFailure(call: Call<List<PaymentResponse>>, t: Throwable) {
                error = "Error de conexión"
                isLoading = false
            }
        })
    }

    LaunchedEffect(Unit) { loadPayments() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Mis Pagos",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
             isLoading -> SkeletonList()
            error != null -> Text(text = error!!, color = MaterialTheme.colorScheme.error)
            pagos.isEmpty() -> Text("No tienes pagos pendientes")
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(pagos) { pago ->
                        PagoCard(
                            pago = pago,
                            onPagado = { loadPayments() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PagoCard(pago: PaymentResponse, onPagado: () -> Unit) {

    val context = LocalContext.current
    var isPaying by remember { mutableStateOf(false) }

    val prefs = context.getSharedPreferences("FuelControlPrefs", Context.MODE_PRIVATE)
    val token = prefs.getString("token", "") ?: ""
    val apiService = Client.getClient(context).create(IPaymentApi::class.java)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (pago.status == "PENDING")
                        Icons.Default.Pending
                    else
                        Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (pago.status == "PENDING")
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pago por $${pago.amount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${pago.fuelType} • ${pago.gallons} galones",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = pago.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (pago.status == "PENDING")
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        }

        if (pago.status == "PENDING") {
            Button(
                onClick = {
                    isPaying = true
                    apiService.payPayment("Bearer $token", pago.id)
                        .enqueue(object : Callback<PaymentResponse> {
                            override fun onResponse(
                                call: Call<PaymentResponse>,
                                response: Response<PaymentResponse>
                            ) {
                                isPaying = false
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "✅ Pago realizado", Toast.LENGTH_SHORT).show()
                                    onPagado()
                                } else {
                                    Toast.makeText(context, "Error al procesar el pago", Toast.LENGTH_SHORT).show()
                                }
                            }
                            override fun onFailure(call: Call<PaymentResponse>, t: Throwable) {
                                isPaying = false
                                Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
                            }
                        })
                },
                enabled = !isPaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                if (isPaying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Pagar")
                }
            }
        }
    }

}
@Composable
fun NotificacionesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("FuelControlPrefs", Context.MODE_PRIVATE)
    val token = "Bearer " + (prefs.getString("token", "") ?: "")

    var alertas by remember { mutableStateOf<List<AlertResponse>>(emptyList()) }
    var alertasLocales by remember { mutableStateOf<List<LocalPriceAlert>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun cargarAlertas() {
        val api = Client.getClient(context).create(IAuthApi::class.java)
        api.getAlerts(token).enqueue(object : Callback<List<AlertResponse>> {
            override fun onResponse(call: Call<List<AlertResponse>>, response: Response<List<AlertResponse>>) {
                isLoading = false
                if (response.isSuccessful && response.body() != null) {
                    alertas = response.body()!!
                } else {
                    error = "Error al cargar notificaciones"
                }
            }
            override fun onFailure(call: Call<List<AlertResponse>>, t: Throwable) {
                isLoading = false
                error = "Error de conexión"
            }
        })
        val prefs = context.getSharedPreferences("FuelControlPrefs", Context.MODE_PRIVATE)
        val guardadas = prefs.getStringSet("price_alerts", emptySet()) ?: emptySet()

        alertasLocales = guardadas.map {
            val partes = it.split("|")
            LocalPriceAlert(
                stationName = partes[0],
                message = partes[0],
                date = partes.getOrNull(1) ?: ""
            )
        }
    }

    LaunchedEffect(Unit) { cargarAlertas() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Notificaciones",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            isLoading -> SkeletonList()
            error != null -> Text(text = error!!, color = MaterialTheme.colorScheme.error)
            alertas.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tienes notificaciones", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {

                val noLeidas = alertas.filter { !it.isRead }
                val leidas = alertas.filter { it.isRead }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (noLeidas.isNotEmpty()) {
                        item {
                            Text("Nuevas", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp))
                        }
                        if (alertasLocales.isNotEmpty()) {
                            item {
                                Text(
                                    "Cambios de precio",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            items(alertasLocales) { alerta ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = alerta.message,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Detectado recientemente",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        items(noLeidas) { alerta ->
                            NotificacionCard(alerta = alerta, onMarcarLeida = {
                                val api = Client.getClient(context).create(IAuthApi::class.java)
                                api.markAsRead(token, alerta.id).enqueue(object : Callback<Void> {
                                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                        if (response.isSuccessful) cargarAlertas()
                                    }
                                    override fun onFailure(call: Call<Void>, t: Throwable) {}
                                })
                            })
                        }
                    }

                    if (leidas.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text("Anteriores", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp))
                        }
                        items(leidas) { alerta ->
                            NotificacionCard(alerta = alerta, onMarcarLeida = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificacionCard(alerta: AlertResponse, onMarcarLeida: (() -> Unit)?) {
    val porcentaje = alerta.percentageAtAlert?.toFloat() ?: 0f
    val color = when {
        porcentaje <= 20f -> MaterialTheme.colorScheme.error
        porcentaje <= 40f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(if (onMarcarLeida != null) 3.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (onMarcarLeida != null)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = if (onMarcarLeida != null) color else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Alerta: ${alerta.fuelType}",
                    fontWeight = if (onMarcarLeida != null) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Nivel al ${porcentaje.toInt()}% — ${alerta.levelAtAlert} gal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = alerta.createdAt ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onMarcarLeida != null) {
                TextButton(onClick = onMarcarLeida) {
                    Text("Leída", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}