package ai.wakehook.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ai.wakehook.app.R
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
            headlineContent = { Text(stringResource(R.string.google_drive)) },
            supportingContent = {
                Text(
                    if (connected) stringResource(R.string.connected_last_sync, formatLastSync(lastSyncMillis))
                    else stringResource(R.string.not_connected)
                )
            },
            trailingContent = {
                if (!connected) Button(onClick = onConnect) { Text(stringResource(R.string.connect)) }
            }
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.sync_now)) },
            trailingContent = { Button(onClick = onSyncNow) { Text(stringResource(R.string.sync)) } }
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.copy_agent_prompt)) },
            supportingContent = { Text(stringResource(R.string.copy_agent_prompt_desc)) },
            trailingContent = { Button(onClick = onCopyPrompt) { Text(stringResource(R.string.copy)) } }
        )
        Divider()
    }
}

private fun formatLastSync(millis: Long): String {
    if (millis <= 0L) return "never"
    val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return fmt.format(Date(millis))
}
