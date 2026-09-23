package ai.wakehook.app.ui.edit

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ai.wakehook.app.R
import ai.wakehook.app.ui.components.PlaybackPreferenceRow

/** On-device ringtone, vibration pattern, and configurable snooze settings. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmPlaybackOptions(settings: AlarmPlaybackSettings, onSettingsChange: (AlarmPlaybackSettings) -> Unit,
    onChooseDeviceRingtone: () -> Unit) {
    var showSoundMenu by remember { mutableStateOf(false) }
    var showVibrationMenu by remember { mutableStateOf(false) }
    var showSnoozeOptions by remember { mutableStateOf(false) }
    val soundLabel = when {
        !settings.soundEnabled || settings.soundUri == AlarmPlaybackSettings.SILENT_SOUND -> stringResource(R.string.sound_off)
        settings.soundUri == AlarmPlaybackSettings.SYSTEM_DEFAULT_SOUND -> stringResource(R.string.sound_system_default)
        else -> stringResource(R.string.sound_device_selected)
    }
    val vibrationLabel = when {
        !settings.vibrationEnabled -> stringResource(R.string.sound_off)
        settings.vibrationPattern == AlarmPlaybackSettings.GENTLE -> stringResource(R.string.vibration_gentle)
        settings.vibrationPattern == AlarmPlaybackSettings.STRONG -> stringResource(R.string.vibration_strong)
        else -> stringResource(R.string.vibration_basic)
    }
    val snoozeLabel = if (!settings.snoozeEnabled) stringResource(R.string.sound_off) else stringResource(
        R.string.snooze_summary, settings.snoozeMinutes,
        if (settings.snoozeLimit == 0) stringResource(R.string.snooze_limit_unlimited)
        else stringResource(R.string.snooze_limit_count, settings.snoozeLimit))

    PlaybackPreferenceRow(stringResource(R.string.alarm_sound), soundLabel, settings.soundEnabled,
        stringResource(R.string.sound_off), { showSoundMenu = true },
        { enabled -> onSettingsChange(settings.copy(soundEnabled = enabled,
            soundUri = if (enabled && settings.soundUri == AlarmPlaybackSettings.SILENT_SOUND)
                AlarmPlaybackSettings.SYSTEM_DEFAULT_SOUND else settings.soundUri)) }) {
        DropdownMenu(expanded = showSoundMenu, onDismissRequest = { showSoundMenu = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.sound_system_default)) }, onClick = {
                onSettingsChange(settings.copy(soundEnabled = true, soundUri = AlarmPlaybackSettings.SYSTEM_DEFAULT_SOUND)); showSoundMenu = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.sound_device)) }, onClick = {
                showSoundMenu = false; onChooseDeviceRingtone()
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.sound_off)) }, onClick = {
                onSettingsChange(settings.copy(soundEnabled = false, soundUri = AlarmPlaybackSettings.SILENT_SOUND)); showSoundMenu = false
            })
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

    PlaybackPreferenceRow(stringResource(R.string.vibration), vibrationLabel, settings.vibrationEnabled,
        stringResource(R.string.sound_off), { showVibrationMenu = true },
        { enabled -> onSettingsChange(settings.copy(vibrationEnabled = enabled)) }) {
        DropdownMenu(expanded = showVibrationMenu, onDismissRequest = { showVibrationMenu = false }) {
            listOf(AlarmPlaybackSettings.BASIC to R.string.vibration_basic,
                AlarmPlaybackSettings.GENTLE to R.string.vibration_gentle,
                AlarmPlaybackSettings.STRONG to R.string.vibration_strong).forEach { (pattern, label) ->
                DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = {
                    onSettingsChange(settings.copy(vibrationEnabled = true, vibrationPattern = pattern)); showVibrationMenu = false
                })
            }
            DropdownMenuItem(text = { Text(stringResource(R.string.sound_off)) }, onClick = {
                onSettingsChange(settings.copy(vibrationEnabled = false)); showVibrationMenu = false
            })
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

    PlaybackPreferenceRow(stringResource(R.string.snooze), snoozeLabel, settings.snoozeEnabled,
        stringResource(R.string.sound_off), { showSnoozeOptions = true },
        { enabled -> onSettingsChange(settings.copy(snoozeEnabled = enabled)) })
    if (showSnoozeOptions) SnoozeOptionsDialog(settings, onDismiss = { showSnoozeOptions = false },
        onSave = { onSettingsChange(it); showSnoozeOptions = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SnoozeOptionsDialog(settings: AlarmPlaybackSettings, onDismiss: () -> Unit, onSave: (AlarmPlaybackSettings) -> Unit) {
    var minutes by remember(settings.snoozeMinutes) { mutableIntStateOf(settings.snoozeMinutes) }
    var limit by remember(settings.snoozeLimit) { mutableIntStateOf(settings.snoozeLimit) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.snooze_options)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.snooze_duration), style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(5, 10, 15, 30).forEach { value -> FilterChip(
                        selected = minutes == value, onClick = { minutes = value },
                        label = { Text(stringResource(R.string.minutes_short, value), maxLines = 1, softWrap = false) },
                        modifier = Modifier.heightIn(min = 44.dp)) }
                }
                Text(stringResource(R.string.snooze_limit), style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(1, 3, 5, 0).forEach { value -> FilterChip(
                        selected = limit == value, onClick = { limit = value },
                        label = { Text(if (value == 0) stringResource(R.string.snooze_limit_unlimited)
                            else stringResource(R.string.snooze_limit_count, value)) }, modifier = Modifier.heightIn(min = 44.dp)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(settings.copy(snoozeEnabled = true, snoozeMinutes = minutes, snoozeLimit = limit)) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}
