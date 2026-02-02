package dev.nutting.kexplore.ui.screen.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.nutting.kexplore.KexploreApp
import dev.nutting.kexplore.data.notification.AlertType
import dev.nutting.kexplore.data.notification.ClusterWatchService
import dev.nutting.kexplore.ui.components.SectionHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringSettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as KexploreApp
    val prefs = remember { app.monitoringPreferences }
    val scope = rememberCoroutineScope()

    val monitoringEnabled by prefs.monitoringEnabled.collectAsState(initial = false)
    val realTimeEnabled by prefs.realTimeEnabled.collectAsState(initial = false)
    val enabledAlertTypes by prefs.enabledAlertTypes.collectAsState(
        initial = AlertType.entries.toSet(),
    )

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasNotificationPermission = granted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monitoring") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        text = "Notification permission is required to receive cluster alerts. Tap the toggle below to grant permission.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            ListItem(
                headlineContent = { Text("Background Monitoring") },
                supportingContent = { Text("Periodic checks every 15 minutes via WorkManager") },
                trailingContent = {
                    Switch(
                        checked = monitoringEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            scope.launch { prefs.setMonitoringEnabled(enabled) }
                        },
                    )
                },
            )

            ListItem(
                headlineContent = { Text("Real-Time Monitoring") },
                supportingContent = { Text("Foreground service with live Kubernetes watches") },
                trailingContent = {
                    Switch(
                        checked = realTimeEnabled,
                        enabled = monitoringEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                prefs.setRealTimeEnabled(enabled)
                                if (enabled) {
                                    val intent = Intent(context, ClusterWatchService::class.java)
                                    context.startForegroundService(intent)
                                } else {
                                    context.stopService(Intent(context, ClusterWatchService::class.java))
                                }
                            }
                        },
                    )
                },
            )

            SectionHeader("Alert Types")

            AlertType.entries.forEach { type ->
                val label = when (type) {
                    AlertType.PodCrashLoop -> "Pod CrashLoopBackOff"
                    AlertType.PodFailed -> "Pod Failed"
                    AlertType.NodeNotReady -> "Node Not Ready"
                    AlertType.DeploymentDegraded -> "Deployment Degraded"
                }
                ListItem(
                    headlineContent = { Text(label) },
                    trailingContent = {
                        Switch(
                            checked = type in enabledAlertTypes,
                            enabled = monitoringEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { prefs.setAlertTypeEnabled(type, enabled) }
                            },
                        )
                    },
                )
            }
        }
    }
}
