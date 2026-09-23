package ai.wakehook.app.ui.edit

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.wakehook.app.R
import ai.wakehook.app.domain.Alarm
import ai.wakehook.app.ui.theme.ScreenHeader
import kotlinx.coroutines.launch
import java.time.LocalDate

private val alarmDraftSaver = Saver<Alarm?, List<Any>>(
    save = { alarm -> alarm?.let { arrayListOf(
        it.id, it.label, it.hour, it.minute, it.repeatDays, ArrayList(it.dates.map(LocalDate::toEpochDay)),
        it.enabled, it.source, it.version, it.ackState, it.ackAt, it.ackError, it.snoozedUntil ?: -1L,
    ) } ?: arrayListOf() },
    restore = { saved -> if (saved.isEmpty()) null else Alarm(
        id = saved[0] as String,
        label = saved[1] as String,
        hour = saved[2] as Int,
        minute = saved[3] as Int,
        repeatDays = saved[4] as Int,
        dates = (saved[5] as List<*>).map { day -> LocalDate.ofEpochDay(day as Long) },
        enabled = saved[6] as Boolean,
        source = saved[7] as String,
        version = saved[8] as Long,
        ackState = saved[9] as String,
        ackAt = saved[10] as String,
        ackError = saved[11] as String,
        snoozedUntil = (saved[12] as Long).takeIf { timestamp -> timestamp >= 0 },
    ) },
)

private val playbackSettingsSaver = Saver<AlarmPlaybackSettings, List<Any>>(
    save = { listOf(it.soundEnabled, it.soundUri ?: AlarmPlaybackSettings.SILENT_SOUND,
        it.vibrationEnabled, it.vibrationPattern, it.snoozeEnabled, it.snoozeMinutes, it.snoozeLimit) },
    restore = { saved -> AlarmPlaybackSettings(
        soundEnabled = saved[0] as Boolean,
        soundUri = saved[1] as String,
        vibrationEnabled = saved[2] as Boolean,
        vibrationPattern = saved[3] as String,
        snoozeEnabled = saved[4] as Boolean,
        snoozeMinutes = saved[5] as Int,
        snoozeLimit = saved[6] as Int,
    ) },
)

/** Coordinates loading, transient editor state, persistence, and navigation for one alarm. */
@Composable
fun AlarmEditScreen(
    vm: AlarmEditViewModel,
    alarmId: String?,
    onDone: () -> Unit,
    onFixExactAlarm: () -> Unit = {},
) {
    val context = LocalContext.current
    val store = remember(context) { AlarmPlaybackSettingsStore(context) }
    var alarm by rememberSaveable(alarmId, stateSaver = alarmDraftSaver) { mutableStateOf<Alarm?>(null) }
    LaunchedEffect(alarmId) { if (alarm == null) alarm = vm.load(alarmId) }
    val currentAlarm = alarm ?: return
    val isNew = alarmId == null
    var mode by rememberSaveable(currentAlarm.id) { mutableStateOf(alarmScheduleMode(currentAlarm)) }
    var playback by rememberSaveable(currentAlarm.id, stateSaver = playbackSettingsSaver) {
        mutableStateOf(store.load(currentAlarm.id, isNew))
    }
    var saving by remember { mutableStateOf(false) }
    var showSaveError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val failureMessage = stringResource(R.string.save_schedule_failed)

    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            playback = playback.copy(
                soundUri = uri?.toString() ?: AlarmPlaybackSettings.SILENT_SOUND,
                soundEnabled = uri != null,
            )
        }
    }
    val chooseRingtone = {
        val selected = playback.soundUri
            ?.takeIf { it != AlarmPlaybackSettings.SILENT_SOUND && it != AlarmPlaybackSettings.SYSTEM_DEFAULT_SOUND }
            ?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.sound_picker_title))
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selected)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        }
        ringtonePicker.launch(intent)
    }

    Scaffold(topBar = {
        ScreenHeader(stringResource(if (isNew) R.string.new_alarm else R.string.edit_alarm), onDone)
    }, bottomBar = {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onDone, enabled = !saving, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                onClick = {
                    val candidate = alarm ?: return@Button
                    if (!isValidSchedule(candidate, mode)) return@Button
                    saving = true
                    showSaveError = false
                    scope.launch {
                        var failed = false
                        try {
                            store.save(candidate.id, playback)
                            vm.save(candidate, onFailure = { failed = true }).join()
                        } catch (_: Exception) {
                            failed = true
                        } finally {
                            saving = false
                        }
                        if (failed) showSaveError = true else onDone()
                    }
                },
                enabled = !saving && isValidSchedule(currentAlarm, mode),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text(stringResource(if (saving) R.string.saving else R.string.save)) }
        }
    }) { insets ->
        Column(
            Modifier.padding(insets).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(top = 2.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(Modifier.fillMaxWidth().height(202.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center) {
                    AlarmTimeWheel(24, currentAlarm.hour, stringResource(R.string.time_wheel_hour_desc),
                        onValueChange = { hour -> alarm = alarm?.copy(hour = hour) }, Modifier.weight(1f))
                    Text(":", fontSize = 38.sp, fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AlarmTimeWheel(60, currentAlarm.minute, stringResource(R.string.time_wheel_minute_desc),
                        onValueChange = { minute -> alarm = alarm?.copy(minute = minute) }, Modifier.weight(1f))
                }
            }
            AlarmEditorOptionsCard(
                alarm = currentAlarm,
                scheduleMode = mode,
                onAlarmChange = { alarm = it },
                onScheduleModeChange = { mode = it },
                playbackSettings = playback,
                onPlaybackSettingsChange = { playback = it },
                onChooseDeviceRingtone = chooseRingtone,
                onLabelChange = { alarm = alarm?.copy(label = it) },
            )
        }
    }

    if (showSaveError) AlertDialog(
        onDismissRequest = { showSaveError = false },
        title = { Text(stringResource(R.string.save_failed_title)) },
        text = { Text(failureMessage) },
        confirmButton = { TextButton(onClick = { showSaveError = false; onFixExactAlarm() }) {
            Text(stringResource(R.string.fix_permission))
        } },
        dismissButton = { TextButton(onClick = { showSaveError = false }) { Text(stringResource(R.string.cancel)) } },
    )
}
