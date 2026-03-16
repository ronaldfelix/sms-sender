package org.aref.smssender

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.aref.smssender.infrastructure.config.AppConfig
import org.aref.smssender.infrastructure.log.SmsLog
import org.aref.smssender.infrastructure.service.SmsServerService
import org.aref.smssender.ui.theme.SmsSenderTheme
import java.net.NetworkInterface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmsSenderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SmsServerScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun SmsServerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appConfig = remember { AppConfig(context) }

    var apiKey by remember { mutableStateOf(appConfig.apiKey) }
    var port by remember { mutableStateOf(appConfig.port.toString()) }
    var serverRunning by remember { mutableStateOf(SmsServerService.isRunning) }
    var selectedSimSlot by remember { mutableIntStateOf(appConfig.defaultSimSlot) }
    val ipAddress = remember { getLocalIpAddress() }
    val availableSims = remember { getActiveSimCards(context) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val logLines by SmsLog.logs.collectAsState()
    val logListState = rememberLazyListState()

    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty()) {
            logListState.animateScrollToItem(logLines.size - 1)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                serverRunning = SmsServerService.isRunning
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            appConfig.apiKey = apiKey
            appConfig.port = port.toIntOrNull() ?: 8080
            appConfig.defaultSimSlot = selectedSimSlot
            val intent = Intent(context, SmsServerService::class.java)
            ContextCompat.startForegroundService(context, intent)
            serverRunning = true
        }
    }

    if (showHelpDialog) {
        HelpDialog(
            apiKey = apiKey,
            ipAddress = ipAddress,
            port = port,
            onDismiss = { showHelpDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (serverRunning) Color(0xFF4CAF50) else Color(0xFFF44336))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SMS Gateway",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = { showHelpDialog = true }) {
                Text("Ayuda")
            }
        }

        if (serverRunning) {
            SelectionContainer {
                Text(
                    text = "http://$ipAddress:$port",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "Configuracion", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        enabled = !serverRunning,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it.filter { c -> c.isDigit() } },
                        label = { Text("Puerto", fontSize = 12.sp) },
                        modifier = Modifier.width(90.dp),
                        enabled = !serverRunning,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "SIM", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(2.dp))

                var simDropdownExpanded by remember { mutableStateOf(false) }

                val simOptions = buildList {
                    add(0 to "Default (sistema)")
                    availableSims.forEach { sim ->
                        add(sim.first to "SIM ${sim.first}: ${sim.second}")
                    }
                }

                val selectedLabel = simOptions.firstOrNull { it.first == selectedSimSlot }?.second
                    ?: "Default (sistema)"

                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .border(
                                width = 1.dp,
                                color = if (serverRunning) MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
                                else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable(enabled = !serverRunning) { simDropdownExpanded = true }
                            .padding(12.dp)
                    ) {
                        Text(
                            text = selectedLabel,
                            fontSize = 13.sp,
                            color = if (serverRunning) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = simDropdownExpanded,
                        onDismissRequest = { simDropdownExpanded = false }
                    ) {
                        simOptions.forEach { (slot, label) ->
                            DropdownMenuItem(
                                text = { Text(label, fontSize = 13.sp) },
                                onClick = {
                                    selectedSimSlot = slot
                                    simDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (availableSims.isEmpty()) {
                    Text(
                        text = "Sin SIMs detectadas (permiso READ_PHONE_STATE requerido)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (serverRunning) {
                    val intent = Intent(context, SmsServerService::class.java)
                    context.stopService(intent)
                    serverRunning = false
                } else {
                    val permissions = mutableListOf(
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.READ_PHONE_STATE
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (serverRunning) Color(0xFFF44336) else Color(0xFF4CAF50)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (serverRunning) "DETENER" else "INICIAR",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2D2D2D))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Logs",
                        color = Color(0xFFCCCCCC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${logLines.size}",
                            color = Color(0xFF888888),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Limpiar",
                            color = Color(0xFF888888),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { SmsLog.clear() }
                                .background(Color(0xFF3D3D3D))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (logLines.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Esperando actividad...",
                            color = Color(0xFF555555),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        state = logListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        items(logLines) { line ->
                            val lineColor = when {
                                "[ERROR]" in line -> Color(0xFFFF6B6B)
                                "[WARN]" in line -> Color(0xFFFFD93D)
                                "[REQ]" in line -> Color(0xFF6BCB77)
                                "[SMS]" in line && "OK" in line -> Color(0xFF4ECDC4)
                                "[SMS]" in line && "FAIL" in line -> Color(0xFFFF6B6B)
                                else -> Color(0xFFB0B0B0)
                            }
                            Text(
                                text = line,
                                color = lineColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HelpDialog(apiKey: String, ipAddress: String, port: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        title = { Text("Ayuda", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                SelectionContainer {
                    Text(
                        text = buildString {
                            appendLine("POST /send-sms  o  /api/sendsms")
                            appendLine()
                            appendLine("Headers:")
                            appendLine("  Content-Type: application/json")
                            appendLine("  x-api-key: $apiKey")
                            appendLine()
                            appendLine("Body:")
                            appendLine("{")
                            appendLine("  \"phone\": \"931023498\",")
                            appendLine("  \"message\": \"Hola mundo\",")
                            appendLine("  \"data_coding\": 8,")
                            appendLine("  \"status\": true,")
                            appendLine("  \"sim_slot\": 1")
                            appendLine("}")
                            appendLine()
                            appendLine("sim_slot (opcional):")
                            appendLine("  omitido = SIM configurada")
                            appendLine("  1 = SIM 1  |  2 = SIM 2")
                            appendLine()
                            appendLine("Acepta \"to\" en vez de \"phone\"")
                            appendLine()
                            appendLine("--- Ejemplo curl ---")
                            appendLine()
                            appendLine("curl -X POST \\")
                            appendLine("  http://$ipAddress:$port/send-sms \\")
                            appendLine("  -H \"Content-Type: application/json\" \\")
                            appendLine("  -H \"x-api-key: $apiKey\" \\")
                            appendLine("  -d '{")
                            appendLine("    \"phone\":\"999999999\",")
                            appendLine("    \"message\":\"Hola\"")
                            append("  }'")
                        },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    )
}

private fun getLocalIpAddress(): String {
    return try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                    return address.hostAddress ?: "0.0.0.0"
                }
            }
        }
        "0.0.0.0"
    } catch (_: Exception) {
        "0.0.0.0"
    }
}

private fun getActiveSimCards(context: android.content.Context): List<Pair<Int, String>> {
    return try {
        val subscriptionManager = context.getSystemService(android.content.Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        @Suppress("MissingPermission")
        val activeList: List<SubscriptionInfo> = subscriptionManager.activeSubscriptionInfoList ?: emptyList()
        activeList.mapIndexed { index, info ->
            val name = info.displayName?.toString() ?: info.carrierName?.toString() ?: "Desconocido"
            @Suppress("DEPRECATION")
            val number = info.number
            val label = if (!number.isNullOrBlank()) "$name ($number)" else name
            (index + 1) to label
        }
    } catch (_: Exception) {
        emptyList()
    }
}
