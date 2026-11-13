package com.nolan.gaminghub

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
private fun Page(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, fontSize = 26.sp, color = MaterialTheme.colorScheme.onBackground)
        content()
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = NavyCard), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 18.sp, color = ElectricBlue)
        }
    }
}

@Composable
fun HomeScreen() {
    val d = remember { readDeviceInfo() }
    Page("Universal Gaming Hub") {
        Text("Your device", color = MaterialTheme.colorScheme.onSurfaceVariant)
        InfoCard("Model", d.model)
        InfoCard("System", d.android)
        InfoCard("Chip", d.chip)
        InfoCard("CPU cores / ABI", "${d.cores} cores / ${d.abi}")
    }
}

@Composable
fun PerformanceScreen() {
    val ctx = LocalContext.current
    var s by remember { mutableStateOf(readLiveStats(ctx)) }
    LaunchedEffect(Unit) { while (true) { s = readLiveStats(ctx); delay(2000) } }
    Page("Performance") {
        InfoCard("RAM", "${s.usedMb} / ${s.totalMb} MB used")
        LinearProgressIndicator(
            progress = { s.usedMb.toFloat() / s.totalMb.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth(), color = ElectricBlue
        )
        InfoCard("Battery", if (s.batteryPct >= 0) "${s.batteryPct}%" else "n/a")
        InfoCard("Battery temperature", "%.1f °C".format(s.tempC))
        Text("Live values, refreshed every 2 s. FPS and GPU data arrive with the emulator stages.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Soon(title: String, stage: String, desc: String) {
    Page(title) {
        InfoCard("Planned in $stage", desc)
        Text("Not implemented yet. No placeholder buttons by design.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
