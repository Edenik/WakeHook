package ai.wakehook.app.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.wakehook.app.R
import ai.wakehook.app.ui.edit.AlarmPlaybackSettings
import ai.wakehook.app.ui.theme.AlarmEmblem
import ai.wakehook.app.ui.theme.ClockTime

/** Full-screen alarm controls, driven entirely by the currently ringing alarm's state. */
@Composable
fun RingScreen(
    time: String,
    label: String,
    playback: AlarmPlaybackSettings,
    snoozesUsed: Int,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(
            listOf(Color(0xFF3A2C27), Color(0xFF0E0F14)), radius = 1100f))) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.app_name), color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge)
                AlarmEmblem()
                ClockTime(time, large = true)
                if (label.isNotBlank()) Text(label, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Button(shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text(stringResource(R.string.dismiss)) }
                if (playback.snoozeEnabled && (playback.snoozeLimit == 0 || snoozesUsed < playback.snoozeLimit)) {
                    OutlinedButton(shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), onClick = onSnooze,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Text(stringResource(R.string.snooze_ring, playback.snoozeMinutes))
                    }
                } else if (playback.snoozeEnabled) {
                    Text(stringResource(R.string.snooze_limit_reached), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
