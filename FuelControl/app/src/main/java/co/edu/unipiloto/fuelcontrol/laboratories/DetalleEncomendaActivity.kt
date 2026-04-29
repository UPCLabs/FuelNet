package co.edu.unipiloto.fuelcontrol.laboratories

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.edu.unipiloto.fuelcontrol.ui.theme.FuelControlTheme

class DetalleEncomendaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val remitente = intent.getStringExtra("remitente") ?: "—"
        val destinatario = intent.getStringExtra("destinatario") ?: "—"
        val direccion = intent.getStringExtra("direccion") ?: "—"
        val descripcion = intent.getStringExtra("descripcion") ?: "—"
        val peso = intent.getStringExtra("peso") ?: "—"

        setContent {
            FuelControlTheme {
                DetalleEncomendaScreen(
                    remitente = remitente,
                    destinatario = destinatario,
                    direccion = direccion,
                    descripcion = descripcion,
                    peso = peso,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleEncomendaScreen(
    remitente: String,
    destinatario: String,
    direccion: String,
    descripcion: String,
    peso: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Características del envío", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text("Detalle de la encomienda",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)

            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetalleItem("📤 Remitente", remitente)
                    HorizontalDivider()
                    DetalleItem("📥 Destinatario", destinatario)
                    HorizontalDivider()
                    DetalleItem("📍 Dirección de destino", direccion)
                    HorizontalDivider()
                    DetalleItem("📦 Descripción", descripcion)
                    HorizontalDivider()
                    DetalleItem("⚖️ Peso", if (peso.isNotBlank()) "$peso kg" else "—")
                }
            }
        }
    }
}

@Composable
fun DetalleItem(label: String, value: String) {
    Column {
        Text(label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium)
    }
}