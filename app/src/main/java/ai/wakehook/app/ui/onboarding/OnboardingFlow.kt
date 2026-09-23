package ai.wakehook.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ai.wakehook.app.R
import ai.wakehook.app.ui.settings.PermissionStatus
import ai.wakehook.app.ui.settings.PermissionRow
import ai.wakehook.app.ui.theme.*

@Composable
fun OnboardingFlow(status: PermissionStatus, driveConnected: Boolean, onFixExactAlarm: () -> Unit,
    onFixNotifications: () -> Unit, onFixBattery: () -> Unit, onFixFullScreen: () -> Unit,
    onConnectDrive: () -> Unit, onCopyPrompt: () -> Unit, onRevoke: () -> Unit, onFinish: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(step > 0) { step-- }
    Scaffold { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
                repeat(4) { i -> Box(Modifier.padding(horizontal = 3.dp).width(if (step == i) 24.dp else 7.dp).height(5.dp)
                    .background(if (step == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))) }
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                when (step) {
                    0 -> {
                        Spacer(Modifier.height(24.dp))
                        AlarmEmblem(Modifier.align(Alignment.CenterHorizontally))
                        SectionLabel(stringResource(R.string.app_name))
                        Text(stringResource(R.string.welcome_headline), style = MaterialTheme.typography.displaySmall)
                        Text(stringResource(R.string.onb_welcome_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    1 -> {
                        Text(stringResource(R.string.onb_perms_title), style = MaterialTheme.typography.headlineLarge)
                        Text(stringResource(R.string.onb_perms_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        PermissionRow(stringResource(R.string.perm_exact_alarms), stringResource(R.string.exact_hint), status.exactAlarm, onFixExactAlarm)
                        PermissionRow(stringResource(R.string.perm_notifications), stringResource(R.string.notification_hint), status.notifications, onFixNotifications)
                        PermissionRow(stringResource(R.string.perm_full_screen), stringResource(R.string.fullscreen_hint), status.fullScreenIntent, onFixFullScreen)
                        PermissionRow(stringResource(R.string.perm_battery), stringResource(R.string.battery_hint), status.batteryExempt, onFixBattery)
                    }
                    2 -> {
                        SectionLabel(stringResource(R.string.optional_connection))
                        Text(stringResource(R.string.connect_headline), style = MaterialTheme.typography.displaySmall)
                        Text(stringResource(R.string.local_connection_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        DesignCard {
                            Text(stringResource(R.string.onb_scope_title), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.drive_scope_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (driveConnected) {
                            Text(stringResource(R.string.onb_connected), color = ReadyGreen)
                            TextButton(shape = RoundedCornerShape(14.dp), onClick = onRevoke) { Text(stringResource(R.string.onb_revoke)) }
                        } else Button(shape = RoundedCornerShape(14.dp), onClick = onConnectDrive, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.onb_sign_in)) }
                    }
                    else -> {
                        AlarmEmblem(Modifier.padding(top = 24.dp))
                        Text(stringResource(R.string.onb_done_title), style = MaterialTheme.typography.displaySmall)
                        Text(stringResource(if (driveConnected) R.string.onb_done_connected else R.string.onb_done_local), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (driveConnected) {
                            DesignCard {
                                SectionLabel(stringResource(R.string.try_asking))
                                Text(stringResource(R.string.example_request), style = MaterialTheme.typography.headlineSmall)
                            }
                            OutlinedButton(shape = RoundedCornerShape(14.dp), onClick = onCopyPrompt, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.copy_agent_prompt)) }
                            Text(stringResource(R.string.remote_timing_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(shape = RoundedCornerShape(14.dp), onClick = { if (step < 3) step++ else onFinish() }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(stringResource(when (step) { 0 -> R.string.onb_get_started; 3 -> R.string.onb_finish; else -> R.string.onb_continue }))
            }
            if (step == 2 && !driveConnected) TextButton(shape = RoundedCornerShape(14.dp), onClick = { step++ }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onb_skip)) }
        }
    }
}
