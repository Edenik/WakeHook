package ai.wakehook.app.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.wakehook.app.R
import ai.wakehook.app.ui.settings.PermissionStatus

/**
 * First-run flow: welcome -> permissions -> connect Drive (with trust scope card) -> done.
 * All side effects (granting permissions, sign-in, copy-prompt) are host callbacks so this
 * stays a pure UI unit. `status` is re-read by the host on resume and passed in fresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingFlow(
    status: PermissionStatus,
    driveConnected: Boolean,
    onFixExactAlarm: () -> Unit,
    onFixNotifications: () -> Unit,
    onFixBattery: () -> Unit,
    onFixFullScreen: () -> Unit,
    onConnectDrive: () -> Unit,
    onCopyPrompt: () -> Unit,
    onRevoke: () -> Unit,
    onFinish: () -> Unit,
) {
    var step by remember { mutableStateOf(0) }
    val total = 4

    Scaffold { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StepDots(step, total)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (step) {
                    0 -> Welcome()
                    1 -> Permissions(status, onFixExactAlarm, onFixNotifications, onFixBattery, onFixFullScreen)
                    2 -> Connect(driveConnected, onConnectDrive, onRevoke)
                    else -> Done(driveConnected, onCopyPrompt)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { if (step < total - 1) step++ else onFinish() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        when (step) {
                            0 -> R.string.onb_get_started
                            total - 1 -> R.string.onb_finish
                            else -> R.string.onb_continue
                        }
                    )
                )
            }
            if (step == 2 && !driveConnected) {
                TextButton(onClick = { step++ }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.onb_skip))
                }
            }
        }
    }
}

@Composable
private fun StepDots(step: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val on = i == step
            Box(Modifier.height(7.dp).width(if (on) 20.dp else 7.dp)) {
                Surface(
                    color = if (on) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {}
            }
        }
    }
}

@Composable
private fun Welcome() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("⏰", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.onb_welcome_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.onb_welcome_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Permissions(
    status: PermissionStatus,
    onFixExactAlarm: () -> Unit,
    onFixNotifications: () -> Unit,
    onFixBattery: () -> Unit,
    onFixFullScreen: () -> Unit,
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.onb_perms_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.onb_perms_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        PermRow(stringResource(R.string.perm_exact_alarms), status.exactAlarm, onFixExactAlarm)
        PermRow(stringResource(R.string.perm_notifications), status.notifications, onFixNotifications)
        PermRow(stringResource(R.string.perm_full_screen), status.fullScreenIntent, onFixFullScreen)
        PermRow(stringResource(R.string.perm_battery), status.batteryExempt, onFixBattery)
    }
}

@Composable
private fun PermRow(label: String, granted: Boolean, onFix: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(stringResource(if (granted) R.string.granted else R.string.required)) },
        leadingContent = {
            if (granted) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = { if (!granted) Button(onClick = onFix) { Text(stringResource(R.string.onb_grant)) } },
    )
}

@Composable
private fun Connect(driveConnected: Boolean, onConnectDrive: () -> Unit, onRevoke: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.onb_connect_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.onb_scope_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.onb_scope_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        if (driveConnected) {
            Text(stringResource(R.string.onb_connected), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRevoke) { Text(stringResource(R.string.onb_revoke)) }
        } else {
            OutlinedButton(onClick = onConnectDrive, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onb_sign_in))
            }
        }
    }
}

@Composable
private fun Done(driveConnected: Boolean, onCopyPrompt: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✓", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.onb_done_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(if (driveConnected) R.string.onb_done_connected else R.string.onb_done_local),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (driveConnected) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onCopyPrompt, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.copy_agent_prompt))
            }
        }
    }
}
