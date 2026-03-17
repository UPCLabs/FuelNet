package co.edu.unipiloto.fuelcontrol

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
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
import androidx.core.net.toUri
import co.edu.unipiloto.fuelcontrol.api.Client
import co.edu.unipiloto.fuelcontrol.api.IStationApi
import co.edu.unipiloto.fuelcontrol.ui.theme.FuelControlTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

data class Gasolinera(
    val id: Long,
    @SerializedName("name")
    val nombre: String,
    @SerializedName("address")
    val direccion: String,
    val latLng: LatLng?,
    var precio: String = "Cargando..."
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

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

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

                AppDestinations.PERFIL -> {
                    Text(
                        text = "Perfil del usuario",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
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

    LaunchedEffect(Unit) {

        try {
            val stations = apiService.getAllStations()

            val gasolinerasConCoords = stations.map { station ->
                val coords = getLatLngFromAddress(context, station.address)

                Gasolinera(
                    id = station.id,
                    nombre = station.name,
                    direccion = station.address,
                    latLng = coords
                )
            }

            gasolineras = gasolinerasConCoords
            isLoading = false

        } catch (e: Exception) {
            Toast.makeText(context, "Error al cargar estaciones", Toast.LENGTH_LONG).show()
            isLoading = false
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    userLocation = latLng
                    cameraPositionState.position =
                        CameraPosition.fromLatLngZoom(latLng, 14f)
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

                                    val response =
                                        apiService.getStationPrices(gasolinera.id)

                                    val precioFormateado =
                                        response.fuels.joinToString("\n") {
                                            "${it.type}: $${it.price}"
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

                            val lat = seleccionada.latLng?.latitude ?: 0.0
                            val lng = seleccionada.latLng?.longitude ?: 0.0

                            val wazeUri =
                                "https://waze.com/ul?ll=$lat,$lng&navigate=yes".toUri()

                            val intent = Intent(Intent.ACTION_VIEW, wazeUri)

                            context.startActivity(intent)
                        }
                    ) {
                        Text("Navegar en Waze")
                    }
                }
            }
        }
    }
}