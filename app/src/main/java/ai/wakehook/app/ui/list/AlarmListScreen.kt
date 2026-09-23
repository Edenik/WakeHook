package ai.wakehook.app.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.wakehook.app.R
import ai.wakehook.app.domain.Alarm
import ai.wakehook.app.domain.AlarmTime
import ai.wakehook.app.domain.hasDay
import ai.wakehook.app.ui.theme.*
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AlarmListScreen(vm: AlarmListViewModel, onAdd: () -> Unit, onEdit: (String) -> Unit,
    onSettings: () -> Unit = {}, exactAlarmAllowed: Boolean = true) {
    val alarms by vm.alarms.collectAsStateWithLifecycle()
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = ZonedDateTime.now(); delay(1000) } }
    val next = nextAlarm(alarms, now)
    Scaffold(topBar = { ScreenHeader(stringResource(R.string.alarms), showBrand = true, actions = {
        IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings)) }
    }) }, floatingActionButton = {
        ExtendedFloatingActionButton(onClick = onAdd, containerColor = MaterialTheme.colorScheme.primary,
            icon = { Icon(Icons.Default.Add, null) }, text = { Text(stringResource(R.string.add_alarm)) })
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 110.dp)) {
            item { SectionLabel(now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))) }
            if (!exactAlarmAllowed) item {
                DesignCard(Modifier.padding(vertical = 12.dp)) {
                    Text(stringResource(R.string.permission_attention), color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = onSettings) { Text(stringResource(R.string.check_permissions)) }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp).background(Sunrise, RoundedCornerShape(24.dp)).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.next_alarm), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    if (next != null) {
                        val time = Instant.ofEpochMilli(next.triggerAt).atZone(now.zone)
                        ClockTime(time.format(DateTimeFormatter.ofPattern("HH:mm")), large = true, accent = true)
                        Text(next.alarm.label.ifBlank { stringResource(R.string.alarms) })
                        Text(time.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(stringResource(R.string.no_active_alarms), style = MaterialTheme.typography.headlineSmall)
                        Text(stringResource(R.string.enable_alarm_hint), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { SectionLabel(stringResource(R.string.your_alarms)) }
            if (alarms.isEmpty()) item {
                DesignCard {
                    AlarmEmblem(Modifier.align(Alignment.CenterHorizontally))
                    Text(stringResource(R.string.fresh_start), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.empty_alarms), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onSettings) { Text(stringResource(R.string.onb_connect_title)) }
                }
            }
            items(alarms, key = { it.id }) { a ->
                Column(Modifier.fillMaxWidth().clickable { onEdit(a.id) }.padding(vertical = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            ClockTime("%02d:%02d".format(Locale.ROOT, a.hour, a.minute))
                            if (a.label.isNotBlank()) Text(a.label, style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(checked = a.enabled, onCheckedChange = { vm.toggle(a) })
                    }
                    Text(scheduleLabel(a), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(when (a.source) {
                        "local" -> stringResource(R.string.set_by_you)
                        "wakehook" -> stringResource(R.string.example_alarm)
                        else -> stringResource(R.string.set_by_agent, a.source)
                    }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    if (!a.enabled) Text(stringResource(R.string.alarm_disabled), style = MaterialTheme.typography.labelSmall)
                    else if (a.ackState == "error") Text(stringResource(R.string.schedule_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item { Text(stringResource(R.string.local_alarm_hint), Modifier.padding(top = 20.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun scheduleLabel(a: Alarm): String = when {
    a.isDateBased -> a.dates.joinToString { it.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())) }
    a.isRecurring -> DayOfWeek.values().filter { a.repeatDays.hasDay(it) }.joinToString(" · ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    else -> stringResource(R.string.mode_once)
}
