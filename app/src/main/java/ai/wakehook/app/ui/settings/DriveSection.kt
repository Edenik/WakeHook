package ai.wakehook.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Google Drive connection status + sync actions, shown above the permission rows in
 * [SettingsScreen]. Purely presentational — callers own connect/sync/copy behavior.
 */
@Composable
fun DriveSection(
    connected: Boolean,
    lastSyncMillis: Long,
    onConnect: () -> Unit,
    onSyncNow: () -> Unit,
    onCopyPrompt: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ListItem(
            headlineContent = { Text("Google Drive") },
            supportingContent = {
                Text(if (connected) "Connected · last sync ${formatLastSync(lastSyncMillis)}" else "Not connected")
            },
            trailingContent = {
                if (!connected) Button(onClick = onConnect) { Text("Connect") }
            }
        )
        ListItem(
            headlineContent = { Text("Sync now") },
            trailingContent = { Button(onClick = onSyncNow) { Text("Sync") } }
        )
        ListItem(
            headlineContent = { Text("Copy agent prompt") },
            supportingContent = { Text("Paste this into your AI agent so it can manage alarms via Drive") },
            trailingContent = { Button(onClick = onCopyPrompt) { Text("Copy") } }
        )
        Divider()
    }
}

private fun formatLastSync(millis: Long): String {
    if (millis <= 0L) return "never"
    val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return fmt.format(Date(millis))
}
