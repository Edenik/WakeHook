package ai.wakehook.app.ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.wakehook.app.R
import ai.wakehook.app.domain.Alarm
import ai.wakehook.app.domain.dayBit
import ai.wakehook.app.domain.groupAlarmDates
import ai.wakehook.app.domain.hasDay
import ai.wakehook.app.ui.utils.dateScheduleLabel
import ai.wakehook.app.ui.utils.displayLabel
import ai.wakehook.app.ui.utils.weeklyScheduleLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val weekDays = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)

enum class AlarmScheduleMode { ONCE, WEEKLY, DATES }

fun alarmScheduleMode(alarm: Alarm) = when {
    alarm.isDateBased -> AlarmScheduleMode.DATES
    alarm.isRecurring -> AlarmScheduleMode.WEEKLY
    else -> AlarmScheduleMode.ONCE
}

fun isValidSchedule(alarm: Alarm, mode: AlarmScheduleMode) = when (mode) {
    AlarmScheduleMode.ONCE -> true
    AlarmScheduleMode.WEEKLY -> alarm.repeatDays != 0
    AlarmScheduleMode.DATES -> alarm.dates.isNotEmpty()
}

/** Date, repeat-day and specific-date controls for an alarm. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlarmSchedulePicker(
    alarm: Alarm,
    mode: AlarmScheduleMode,
    onAlarmChange: (Alarm) -> Unit,
    onModeChange: (AlarmScheduleMode) -> Unit,
) {
    var showDateRangePicker by rememberSaveable(alarm.id) { mutableStateOf(false) }
    val locale = Locale.getDefault()
    val selectedDescription = stringResource(R.string.selected)
    val notSelectedDescription = stringResource(R.string.not_selected)

    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(scheduleSummary(alarm, mode, locale), Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        IconButton(onClick = { showDateRangePicker = true }) {
            Icon(Icons.Default.CalendarMonth, stringResource(R.string.choose_dates), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (mode == AlarmScheduleMode.DATES) {
        FlowRow(Modifier.fillMaxWidth().padding(bottom = 7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            groupAlarmDates(alarm.dates).forEach { range ->
                InputChip(selected = false, onClick = {
                    val remaining = alarm.dates - range.dates().toSet()
                    onAlarmChange(alarm.copy(dates = remaining))
                    if (remaining.isEmpty()) onModeChange(AlarmScheduleMode.ONCE)
                },
                    label = { Text(range.displayLabel(locale)) },
                    trailingIcon = { Icon(Icons.Default.Close, stringResource(R.string.remove_date_range, range.displayLabel(locale))) },
                    modifier = Modifier.heightIn(min = 44.dp))
            }
            if (alarm.dates.isEmpty()) Text(stringResource(R.string.add_date_range_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterVertically))
        }
    } else {
        Row(Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            weekDays.forEach { day ->
                val checked = mode == AlarmScheduleMode.WEEKLY && alarm.repeatDays.hasDay(day)
                val weekend = day == DayOfWeek.SUNDAY || day == DayOfWeek.SATURDAY
                Surface(onClick = {
                    onModeChange(AlarmScheduleMode.WEEKLY)
                    val updated = alarm.copy(repeatDays = alarm.repeatDays xor dayBit(day), dates = emptyList())
                    onAlarmChange(updated)
                    if (updated.repeatDays == 0) onModeChange(AlarmScheduleMode.ONCE)
                }, modifier = Modifier.weight(1f).heightIn(min = 44.dp).semantics {
                    role = Role.Checkbox
                    selected = checked
                    stateDescription = if (checked) selectedDescription else notSelectedDescription
                }, shape = CircleShape,
                    color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(day.getDisplayName(TextStyle.NARROW, locale), style = MaterialTheme.typography.labelMedium,
                            color = if (checked) MaterialTheme.colorScheme.onPrimary else if (weekend)
                                MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }

    if (showDateRangePicker) AlarmDatePicker(
        selectedDates = alarm.dates,
        onDismiss = { showDateRangePicker = false },
        onAddDates = { ranges ->
            val addedDates = ranges.flatMap { it.dates() }
            onAlarmChange(alarm.copy(repeatDays = 0, dates = (alarm.dates + addedDates).distinct().sorted()))
            onModeChange(AlarmScheduleMode.DATES)
        },
    )
}

@Composable
private fun scheduleSummary(alarm: Alarm, mode: AlarmScheduleMode, locale: Locale): String = when {
    mode == AlarmScheduleMode.DATES -> dateScheduleLabel(alarm.dates, locale)
    mode == AlarmScheduleMode.WEEKLY -> weeklyScheduleLabel(alarm.repeatDays, locale)
    else -> stringResource(R.string.schedule_once_label,
        LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("EEE, MMM d", locale)))
}
