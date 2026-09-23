package ai.wakehook.app.ui.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ai.wakehook.app.R
import ai.wakehook.app.domain.Alarm

/** Groups recurrence, date and playback choices in the alarm editor's settings card. */
@Composable
fun AlarmEditorOptionsCard(
    alarm: Alarm,
    scheduleMode: AlarmScheduleMode,
    onAlarmChange: (Alarm) -> Unit,
    onScheduleModeChange: (AlarmScheduleMode) -> Unit,
    playbackSettings: AlarmPlaybackSettings,
    onPlaybackSettingsChange: (AlarmPlaybackSettings) -> Unit,
    onChooseDeviceRingtone: () -> Unit,
    onLabelChange: (String) -> Unit,
) {
    Card(shape = RoundedCornerShape(25.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            AlarmSchedulePicker(alarm, scheduleMode, onAlarmChange, onScheduleModeChange)
            OutlinedTextField(value = alarm.label, onValueChange = onLabelChange,
                placeholder = { Text(stringResource(R.string.alarm_name)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 3.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AlarmPlaybackOptions(playbackSettings, onPlaybackSettingsChange, onChooseDeviceRingtone)
            Spacer(Modifier.height(3.dp))
        }
    }
    Text(stringResource(R.string.device_alarm_options_hint), Modifier.padding(horizontal = 4.dp),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
