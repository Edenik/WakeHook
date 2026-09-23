package ai.wakehook.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ai.wakehook.app.R

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
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings)) }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DriveSection(
                connected = driveConnected,
                lastSyncMillis = lastSyncMillis,
                onConnect = onConnectDrive,
                onSyncNow = onSyncNow,
                onCopyPrompt = onCopyPrompt,
            )
            PermRow(stringResource(R.string.perm_exact_alarms), status.exactAlarm, onFixExactAlarm)
            PermRow(stringResource(R.string.perm_notifications), status.notifications, onFixNotifications)
            PermRow(stringResource(R.string.perm_battery), status.batteryExempt, onFixBattery)
            PermRow(stringResource(R.string.perm_full_screen), status.fullScreenIntent, onFixFullScreenIntent)
        }
    }
}

@Composable
private fun PermRow(label: String, granted: Boolean, onFix: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(stringResource(if (granted) R.string.granted else R.string.required)) },
        trailingContent = { if (!granted) Button(onClick = onFix) { Text(stringResource(R.string.fix)) } }
    )
}
