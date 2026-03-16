package co.edu.unipiloto.fuelcontrol

import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.edu.unipiloto.fuelcontrol.ui.theme.FuelControlTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.core.net.toUri
import co.edu.unipiloto.fuelcontrol.api.Client
import co.edu.unipiloto.fuelcontrol.api.IStationApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import androidx.compose.material3.CircularProgressIndicator
import com.google.gson.annotations.SerializedName

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    icon = {
                        Icon(it.icon, contentDescription = it.label)
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {

        val context = LocalContext.current;

        val apiService = remember {
            Client.getClient(context).create(IStationApi::class.java)
        }

        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->

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
            Text("Consultar precios ")
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

            if (results != null && results.isNotEmpty()) {
                val location = results[0]
                LatLng(location.latitude, location.longitude)
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}

@Composable
fun MapScreen(modifier: Modifier = Modifier, apiService: IStationApi) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estado para la lista de gasolineras con coordenadas
    var gasolineras by remember { mutableStateOf<List<Gasolinera>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Estado para la selección y detalle de precios
    var gasolineraSeleccionada by remember { mutableStateOf<Gasolinera?>(null) }

    val bogota = LatLng(4.7110, -74.0721)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(bogota, 12f)
    }

    // 1. Cargar datos iniciales al entrar a la pantalla
    LaunchedEffect(Unit) {
        try {
            // Llamada al endpoint GET /stations
            val stations = apiService.getAllStations()

            // Convertir direcciones a LatLng en paralelo (Geocoding)
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
            println(e.message)
            Toast.makeText(context, "Error al cargar estaciones: ${e.message}", Toast.LENGTH_LONG).show()
            isLoading = false
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
                                    val precioFormateado = response.fuels.joinToString("\n") {
                                        "${it.type}: $${it.price}"
                                    }
                                    gasolineraSeleccionada = gasolinera.copy(precio = precioFormateado)
                                } catch (e: Exception) {
                                    gasolineraSeleccionada = gasolinera.copy(precio = "Error al cargar precios")
                                }
                            }

                            cameraPositionState.position = CameraPosition.fromLatLngZoom(position, 15f)
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
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = seleccionada.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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

                            val wazeUri = "https://waze.com/ul?ll=$lat,$lng&navigate=yes".toUri()
                            val wazeIntent = Intent(Intent.ACTION_VIEW, wazeUri)

                            if (wazeIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(wazeIntent)
                            } else {
                                val googleUri = "google.navigation:q=$lat,$lng".toUri()
                                val googleIntent = Intent(Intent.ACTION_VIEW, googleUri)
                                context.startActivity(googleIntent)
                            }
                        }
                    ) {
                        Text("Navegar en Waze")
                    }
                }
            }
        }
    }
}