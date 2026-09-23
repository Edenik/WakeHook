package ai.wakehook.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ai.wakehook.app.R
import ai.wakehook.app.ui.theme.*

@Composable
fun SettingsScreen(status: PermissionStatus, onFixExactAlarm: () -> Unit, onFixNotifications: () -> Unit,
    onFixBattery: () -> Unit, onFixFullScreenIntent: () -> Unit, driveConnected: Boolean = false,
    lastSyncMillis: Long = 0L, onConnectDrive: () -> Unit = {}, onSyncNow: () -> Unit = {},
    onCopyPrompt: () -> Unit = {}, onBack: () -> Unit = {}) {
    Scaffold(topBar = { ScreenHeader(stringResource(R.string.settings), onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel(stringResource(R.string.agent_connection))
            DriveSection(driveConnected, lastSyncMillis, onConnectDrive, onSyncNow, onCopyPrompt)
            SectionLabel(stringResource(R.string.alarm_readiness))
            DesignCard {
                PermissionRow(stringResource(R.string.perm_exact_alarms), stringResource(R.string.exact_hint), status.exactAlarm, onFixExactAlarm)
                PermissionRow(stringResource(R.string.perm_notifications), stringResource(R.string.notification_hint), status.notifications, onFixNotifications)
                PermissionRow(stringResource(R.string.perm_full_screen), stringResource(R.string.fullscreen_hint), status.fullScreenIntent, onFixFullScreenIntent)
                PermissionRow(stringResource(R.string.perm_battery), stringResource(R.string.battery_hint), status.batteryExempt, onFixBattery)
            }
            Text(stringResource(R.string.local_alarm_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PermissionRow(label: String, description: String, granted: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (granted) Text(stringResource(R.string.granted), color = ReadyGreen, style = MaterialTheme.typography.labelSmall)
        else OutlinedButton(shape = RoundedCornerShape(14.dp), onClick = onFix) { Text(stringResource(R.string.fix)) }
    }
}
