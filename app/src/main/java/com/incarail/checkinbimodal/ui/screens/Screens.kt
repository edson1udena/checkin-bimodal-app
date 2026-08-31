package com.incarail.checkinbimodal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.incarail.checkinbimodal.data.BoardingStatus
import com.incarail.checkinbimodal.data.Frequency
import com.incarail.checkinbimodal.data.FrequencyStatus
import com.incarail.checkinbimodal.data.Passenger
import com.incarail.checkinbimodal.ui.theme.StatusColors
import com.incarail.checkinbimodal.viewmodel.CheckinViewModel

/** Pantalla 1 — Inicio de sesión simple por PIN (prototipo: cualquier PIN de 4 dígitos entra). */
@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(pin) {
        if (pin.length == 4) {
            onLoggedIn()
            pin = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Check-in Bimodal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Ingresa tu PIN de colaborador SAB",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
            repeat(4) { i ->
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (i < pin.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            keys.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            OutlinedButton(
                                onClick = {
                                    pin = when (key) {
                                        "⌫" -> pin.dropLast(1)
                                        else -> if (pin.length < 4) pin + key else pin
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) { Text(key, style = MaterialTheme.typography.titleMedium) }
                        }
                    }
                }
            }
        }
    }
}

/** Pantalla 2 — Frecuencias del día asignadas al colaborador. */
@Composable
fun FrequencyListScreen(viewModel: CheckinViewModel, onSelect: (Frequency) -> Unit) {
    LaunchedEffect(Unit) { viewModel.loadFrequencies() }
    val frequencies by viewModel.frequencies

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Frecuencias de hoy") })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(frequencies) { freq ->
                FrequencyCard(freq, onClick = { onSelect(freq) })
            }
        }
    }
}

@Composable
private fun FrequencyCard(freq: Frequency, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(freq.time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(freq.route, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill(freq.status)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Unidad ${freq.unit} · ${freq.expectedPax} pasajeros esperados",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StatusPill(status: FrequencyStatus) {
    val (label, color) = when (status) {
        FrequencyStatus.PENDING -> "Pendiente" to StatusColors.warn
        FrequencyStatus.IN_PROGRESS -> "En curso" to MaterialTheme.colorScheme.primary
        FrequencyStatus.CLOSED -> "Cerrada" to StatusColors.good
    }
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
        Text(label, color = color, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

/** Pantalla 3 — Lista de pasajeros esperados + registro de abordaje. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerListScreen(
    viewModel: CheckinViewModel,
    onBack: () -> Unit,
    onClosed: () -> Unit,
) {
    val freq = viewModel.selectedFrequency.value ?: return
    val query by viewModel.searchQuery
    var showAddDialog by remember { mutableStateOf(false) }
    val passengers = viewModel.filteredPassengers()
    val boarded = viewModel.passengers.value.count { it.status == BoardingStatus.BOARDED }
    val noShow = viewModel.passengers.value.count { it.status == BoardingStatus.NO_SHOW }
    val pending = viewModel.passengers.value.size - boarded - noShow

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${freq.time} · ${freq.unit}") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Añadir pasajero") }
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = { viewModel.closeManifest(); onClosed() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) { Text("Cerrar manifiesto de abordaje") }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile("Abordó", boarded, Modifier.weight(1f))
                StatTile("No show", noShow, Modifier.weight(1f))
                StatTile("Pendiente", pending, Modifier.weight(1f))
            }
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Buscar por nombre o documento") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(passengers, key = { it.id }) { pax ->
                    PassengerRow(
                        pax,
                        onBoard = { viewModel.setStatus(pax.id, BoardingStatus.BOARDED) },
                        onNoShow = { viewModel.setStatus(pax.id, BoardingStatus.NO_SHOW) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddWalkInDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, doc, reason ->
                viewModel.addWalkIn(name, doc, reason)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp), modifier = modifier) {
        Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label.uppercase(), color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PassengerRow(pax: Passenger, onBoard: () -> Unit, onNoShow: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            val initials = pax.name.split(" ").let { (it.getOrNull(0)?.take(1) ?: "") + (it.getOrNull(1)?.take(1) ?: "") }
            Text(initials.uppercase(), style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(pax.name + if (pax.isWalkIn) "  ·  Walk-in" else "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("${pax.document} · ${pax.type}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onBoard) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Abordó",
                tint = if (pax.status == BoardingStatus.BOARDED) StatusColors.good else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onNoShow) {
            Icon(
                Icons.Default.Close,
                contentDescription = "No abordó",
                tint = if (pax.status == BoardingStatus.NO_SHOW) StatusColors.critical else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Divider()
}

@Composable
private fun AddWalkInDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var doc by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Reubicado de otra frecuencia") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir pasajero no listado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre completo") }, singleLine = true)
                OutlinedTextField(value = doc, onValueChange = { doc = it }, label = { Text("Documento") }, singleLine = true)
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Motivo") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, doc, reason) }) { Text("Agregar y marcar abordó") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Pantalla 4 — Resumen de cierre del manifiesto. */
@Composable
fun SummaryScreen(viewModel: CheckinViewModel, onDone: () -> Unit) {
    val summary = viewModel.lastSummary.value
    val freq = viewModel.selectedFrequency.value

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("MANIFIESTO CERRADO", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
            Text(
                (summary?.boarded ?: 0).toString(),
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "pasajeros a bordo · ${freq?.time ?: ""} ${freq?.unit ?: ""}",
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile("Abordó", summary?.boarded ?: 0, StatusColors.good, Modifier.weight(1f))
            SummaryTile("No show", summary?.noShow ?: 0, StatusColors.critical, Modifier.weight(1f))
            SummaryTile("Adicionales", summary?.additional ?: 0, StatusColors.warn, Modifier.weight(1f))
        }
        Text(
            "Enviado a JESAB · GOP · Backend Inca Rail",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onDone, modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) { Text("Volver a frecuencias") }
    }
}

@Composable
private fun SummaryTile(label: String, value: Int, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), color = color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall)
        }
    }
}
