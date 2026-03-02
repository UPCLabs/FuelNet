package co.edu.unipiloto.fuelcontrol

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import co.edu.unipiloto.fuelcontrol.ui.theme.FuelControlTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.core.net.toUri

data class Gasolinera(
    val nombre: String,
    val direccion: String,
    val precio: Double,
    val lat: Double,
    val lng: Double
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
                    MapScreen(Modifier.padding(innerPadding))
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

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Inicio", Icons.Default.Home),
    MAPA("Mapa", Icons.Default.Home),
    PERFIL("Perfil", Icons.Default.AccountBox),
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

@Composable
fun MapScreen(modifier: Modifier = Modifier) {

    val context = LocalContext.current

    val gasolineras = listOf(
        Gasolinera(
            "Terpel Centro",
            "Cra 7 # 32-45",
            12450.0,
            4.7110,
            -74.0721
        ),
        Gasolinera(
            "Primax Norte",
            "Calle 100 # 15-20",
            12300.0,
            4.6880,
            -74.0500
        )
    )

    val bogota = LatLng(4.7110, -74.0721)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(bogota, 12f)
    }

    Box(modifier = modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {

            gasolineras.forEach { gasolinera ->

                Marker(
                    state = MarkerState(
                        position = LatLng(
                            gasolinera.lat,
                            gasolinera.lng
                        )
                    ),
                    title = gasolinera.nombre,
                    snippet = "Precio: $${gasolinera.precio}"
                )
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                val seleccionada = gasolineras[0]

                Text(seleccionada.nombre)
                Text(seleccionada.direccion)
                Text("Precio: $${seleccionada.precio}")

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {

                        val wazeUri =
                            "https://waze.com/ul?ll=${seleccionada.lat},${seleccionada.lng}&navigate=yes"
                                .toUri()
                        val wazeIntent = Intent(Intent.ACTION_VIEW, wazeUri)

                        if (wazeIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(wazeIntent)
                        } else {
                            val googleUri =
                                "google.navigation:q=${seleccionada.lat},${seleccionada.lng}"
                                    .toUri()
                            val googleIntent = Intent(Intent.ACTION_VIEW, googleUri)
                            context.startActivity(googleIntent)
                        }
                    }
                ) {
                    Text("Abrir en Waze ")
                }
            }
        }
    }
}