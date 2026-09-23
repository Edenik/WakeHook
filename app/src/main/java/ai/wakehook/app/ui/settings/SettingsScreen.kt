package ai.wakehook.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    status: PermissionStatus,
    onFixExactAlarm: () -> Unit,
    onFixNotifications: () -> Unit,
    onFixBattery: () -> Unit,
    onFixFullScreenIntent: () -> Unit,
    driveConnected: Boolean = false,
    lastSyncMillis: Long = 0L,
    onConnectDrive: () -> Unit = {},
    onSyncNow: () -> Unit = {},
    onCopyPrompt: () -> Unit = {},
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DriveSection(
                connected = driveConnected,
                lastSyncMillis = lastSyncMillis,
                onConnect = onConnectDrive,
                onSyncNow = onSyncNow,
                onCopyPrompt = onCopyPrompt,
            )
            PermRow("Exact alarms", status.exactAlarm, onFixExactAlarm)
            PermRow("Notifications", status.notifications, onFixNotifications)
            PermRow("Ignore battery optimization", status.batteryExempt, onFixBattery)
            PermRow("Full-screen alarms", status.fullScreenIntent, onFixFullScreenIntent)
        }
    }
}

@Composable
private fun PermRow(label: String, granted: Boolean, onFix: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(if (granted) "Granted" else "Required") },
        trailingContent = { if (!granted) Button(onClick = onFix) { Text("Fix") } }
    )
}
