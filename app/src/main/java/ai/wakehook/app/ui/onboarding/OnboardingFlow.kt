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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
                    when (step) {
                        0 -> "Get started"
                        total - 1 -> "Finish"
                        else -> "Continue"
                    }
                )
            }
            if (step == 2 && !driveConnected) {
                TextButton(onClick = { step++ }, modifier = Modifier.fillMaxWidth()) {
                    Text("Skip for now — use locally")
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
            Box(
                Modifier
                    .height(7.dp)
                    .width(if (on) 20.dp else 7.dp)
                    .then(Modifier)
            ) {
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
        Text(
            "Alarms your AI agent can set.",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "A real alarm clock that also takes orders from your assistant — through a file in your own Google Drive. No WakeHook account, no server.",
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
        Text("Permissions to ring reliably", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Android needs these so an alarm fires even on silent, asleep, or after a reboot.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        PermRow("Exact alarms", status.exactAlarm, onFixExactAlarm)
        PermRow("Notifications", status.notifications, onFixNotifications)
        PermRow("Full-screen alarms", status.fullScreenIntent, onFixFullScreen)
        PermRow("Ignore battery optimization", status.batteryExempt, onFixBattery)
    }
}

@Composable
private fun PermRow(label: String, granted: Boolean, onFix: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(if (granted) "Granted" else "Required") },
        leadingContent = {
            if (granted) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = { if (!granted) Button(onClick = onFix) { Text("Grant") } },
    )
}

@Composable
private fun Connect(driveConnected: Boolean, onConnectDrive: () -> Unit, onRevoke: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("Connect your Google Drive", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        // Trust scope card
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("What WakeHook can access", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Only the one file it creates (scope: drive.file). It cannot see, read, or open anything else in your Drive. No WakeHook server — your alarms stay in your account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        if (driveConnected) {
            Text("✓ Connected", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRevoke) { Text("Revoke access") }
        } else {
            OutlinedButton(onClick = onConnectDrive, modifier = Modifier.fillMaxWidth()) {
                Text("Sign in with Google")
            }
        }
    }
}

@Composable
private fun Done(driveConnected: Boolean, onCopyPrompt: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✓", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("You're set", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            if (driveConnected)
                "Created WakeHook/wakehook.json + wakehook.md in your Drive, with example alarms (off). Copy the agent prompt so your assistant can set alarms."
            else
                "You can add alarms now. Connect Google Drive later in Settings to let an agent set them.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (driveConnected) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onCopyPrompt, modifier = Modifier.fillMaxWidth()) {
                Text("Copy agent prompt")
            }
        }
    }
}
