package co.edu.unipiloto.fuelcontrol.laboratories

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import co.edu.unipiloto.fuelcontrol.NotificationHelper
import co.edu.unipiloto.fuelcontrol.ui.theme.FuelControlTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class EncomendaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FuelControlTheme {
                EncomendaScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncomendaScreen() {
    val context = LocalContext.current
    val db = remember { EncomendaDbHelper(context) }

    var remitente by remember { mutableStateOf("") }
    var destinatario by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var encomiendas by remember { mutableStateOf<List<Encomienda>>(emptyList()) }
    var mostrarLista by remember { mutableStateOf(false) }

    fun limpiarFormulario() {
        remitente = ""
        destinatario = ""
        direccion = ""
        descripcion = ""
        peso = ""
    }

    fun textoParaCompartir() =
        """
        📦 SOLICITUD DE ENVÍO DE ENCOMIENDA
        
        Remitente: $remitente
        Destinatario: $destinatario
        Dirección destino: $direccion
        Descripción: $descripcion
        Peso: ${peso}kg
        Fecha: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}
        """.trimIndent()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registro de Encomienda", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(context, DetalleEncomendaActivity::class.java).apply {
                            putExtra("remitente", remitente)
                            putExtra("destinatario", destinatario)
                            putExtra("direccion", direccion)
                            putExtra("descripcion", descripcion)
                            putExtra("peso", peso)
                        }
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Info, contentDescription = "Ver características")
                    }

                    IconButton(onClick = {
                        if (remitente.isBlank() || destinatario.isBlank()) {
                            Toast.makeText(context, "Llena remitente y destinatario primero", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }

                        NotificationHelper.createChannel(context)

                        val notification = androidx.core.app.NotificationCompat.Builder(context, "fuel_alerts")
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentTitle("Nueva encomienda registrada")
                            .setContentText("Para: $destinatario en $direccion")
                            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                            .setAutoCancel(true)
                            .build()

                        val manager = androidx.core.app.NotificationManagerCompat.from(context)
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                context, android.Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            manager.notify(System.currentTimeMillis().toInt(), notification)
                        }
                    }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notificar")
                    }
                }
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            item {
                Spacer(Modifier.height(8.dp))
                Text("Datos del envío", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
            }

            item {
                OutlinedTextField(
                    value = remitente,
                    onValueChange = { remitente = it },
                    label = { Text("Remitente") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = destinatario,
                    onValueChange = { destinatario = it },
                    label = { Text("Destinatario") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("Dirección de destino") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción del paquete") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            item {
                OutlinedTextField(
                    value = peso,
                    onValueChange = { peso = it },
                    label = { Text("Peso (kg)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            if (mensaje.isNotEmpty()) {
                item {
                    Text(
                        text = mensaje,
                        color = if (mensaje.startsWith("✓"))
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (remitente.isBlank() || destinatario.isBlank() ||
                                direccion.isBlank() || descripcion.isBlank() || peso.isBlank()
                            ) {
                                mensaje = "Completa todos los campos"
                                return@Button
                            }
                            val pesoDouble = peso.toDoubleOrNull()
                            if (pesoDouble == null) {
                                mensaje = "El peso debe ser un número válido"
                                return@Button
                            }
                            val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                .format(Date())
                            val id = db.insertar(
                                Encomienda(
                                    remitente = remitente,
                                    destinatario = destinatario,
                                    direccionDestino = direccion,
                                    descripcion = descripcion,
                                    peso = pesoDouble,
                                    fecha = fecha
                                )
                            )
                            if (id > 0) {
                                mensaje = "✓ Encomienda guardada (ID: $id)"
                                limpiarFormulario()
                            } else {
                                mensaje = "Error al guardar"
                            }
                        }
                    ) {
                        Text("Guardar")
                    }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            encomiendas = db.consultarTodas()
                            mostrarLista = true
                            if (encomiendas.isEmpty()) mensaje = "No hay encomiendas registradas"
                        }
                    ) {
                        Text("Consultar")
                    }
                }
            }

            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (remitente.isBlank() || destinatario.isBlank()) {
                            mensaje = "Completa al menos remitente y destinatario"
                            return@Button
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Solicitud de envío de encomienda")
                            putExtra(Intent.EXTRA_TEXT, textoParaCompartir())
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Compartir encomienda via...")
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Compartir")
                }
            }

            if (mostrarLista && encomiendas.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Encomiendas registradas (${encomiendas.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(encomiendas) { enc ->
                    EncomendaCard(enc)
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun EncomendaCard(enc: Encomienda) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("# ${enc.id}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(enc.fecha, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text("De: ${enc.remitente}", fontWeight = FontWeight.Bold)
            Text("Para: ${enc.destinatario}")
            Text("Destino: ${enc.direccionDestino}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${enc.descripcion} • ${enc.peso} kg",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}