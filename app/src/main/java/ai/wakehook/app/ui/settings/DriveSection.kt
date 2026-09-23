package ai.wakehook.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ai.wakehook.app.R
import ai.wakehook.app.ui.theme.*
import java.text.DateFormat
import java.util.Date

@Composable
fun DriveSection(connected: Boolean, lastSyncMillis: Long, onConnect: () -> Unit, onSyncNow: () -> Unit, onCopyPrompt: () -> Unit) {
    var showInstructions by remember { mutableStateOf(false) }
    var requestedAt by remember { mutableStateOf(0L) }
    val pending = requestedAt > lastSyncMillis
    DesignCard {
        Text(stringResource(R.string.google_drive), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(if (connected) R.string.onb_connected else R.string.not_connected),
            color = if (connected) ReadyGreen else MaterialTheme.colorScheme.onSurfaceVariant)
        if (connected) {
            Text(if (lastSyncMillis > 0) stringResource(R.string.connected_last_sync, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(lastSyncMillis)))
                else stringResource(R.string.waiting_first_sync), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(shape = RoundedCornerShape(14.dp), onClick = { requestedAt = System.currentTimeMillis(); onSyncNow() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (pending) R.string.sync_requested else R.string.sync_now))
            }
            Text(stringResource(R.string.remote_timing_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(stringResource(R.string.local_connection_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(shape = RoundedCornerShape(14.dp), onClick = onConnect, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onb_sign_in)) }
        }
    }
    if (connected) {
        OutlinedButton(shape = RoundedCornerShape(14.dp), onClick = { showInstructions = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(stringResource(R.string.copy_agent_prompt))
        }
    }
    if (showInstructions) AlertDialog(onDismissRequest = { showInstructions = false },
        title = { Text(stringResource(R.string.agent_handoff_title)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.agent_handoff_steps))
            Text(stringResource(R.string.remote_timing_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } },
        confirmButton = { TextButton(shape = RoundedCornerShape(14.dp), onClick = { onCopyPrompt(); showInstructions = false }) { Text(stringResource(R.string.copy)) } },
        dismissButton = { TextButton(shape = RoundedCornerShape(14.dp), onClick = { showInstructions = false }) { Text(stringResource(R.string.cancel)) } })
}
