package ai.wakehook.app.ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ai.wakehook.app.R
import ai.wakehook.app.domain.Alarm
import ai.wakehook.app.domain.hasDay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class EditMode { ONCE, WEEKLY, DATES }

private fun modeOf(a: Alarm): EditMode = when {
    a.isDateBased -> EditMode.DATES
    a.isRecurring -> EditMode.WEEKLY
    else -> EditMode.ONCE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(vm: AlarmEditViewModel, alarmId: String?, onDone: () -> Unit) {
    var alarm by remember { mutableStateOf<Alarm?>(null) }
    LaunchedEffect(alarmId) { alarm = vm.load(alarmId) }
    val a = alarm ?: return
    // Mode is explicit UI state (seeded from the loaded alarm), NOT derived from
    // data — otherwise you could never leave "Once": Weekly needs a day and Dates
    // needs a date, but those controls only show once you're already in that mode.
    var mode by remember(a.id) { mutableStateOf(modeOf(a)) }
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(if (alarmId == null) R.string.new_alarm else R.string.edit_alarm)) },
            actions = { TextButton(onClick = { vm.save(a); onDone() }) { Text(stringResource(R.string.save)) } }
        )
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = a.hour.toString(),
                    onValueChange = { s -> s.toIntOrNull()?.takeIf { it in 0..23 }?.let { alarm = a.copy(hour = it) } },
                    label = { Text(stringResource(R.string.hour)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = a.minute.toString(),
                    onValueChange = { s -> s.toIntOrNull()?.takeIf { it in 0..59 }?.let { alarm = a.copy(minute = it) } },
                    label = { Text(stringResource(R.string.minute)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = a.label, onValueChange = { alarm = a.copy(label = it) },
                label = { Text(stringResource(R.string.label)) }, modifier = Modifier.fillMaxWidth()
            )

            Text(stringResource(R.string.mode))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = mode == EditMode.ONCE,
                    onClick = { mode = EditMode.ONCE; alarm = a.copy(repeatDays = 0, dates = emptyList()) },
                    label = { Text(stringResource(R.string.mode_once)) }
                )
                FilterChip(
                    selected = mode == EditMode.WEEKLY,
                    onClick = { mode = EditMode.WEEKLY; alarm = a.copy(dates = emptyList()) },
                    label = { Text(stringResource(R.string.mode_weekly)) }
                )
                FilterChip(
                    selected = mode == EditMode.DATES,
                    onClick = { mode = EditMode.DATES; alarm = a.copy(repeatDays = 0) },
                    label = { Text(stringResource(R.string.mode_dates)) }
                )
            }

            if (mode == EditMode.WEEKLY) {
                Text(stringResource(R.string.repeat))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayOfWeek.values().forEach { day ->
                        FilterChip(
                            selected = a.repeatDays.hasDay(day),
                            onClick = { alarm = vm.toggleDay(a, day) },
                            label = { Text(day.name.take(2)) }
                        )
                    }
                }
            }

            if (mode == EditMode.DATES) {
                Text(stringResource(R.string.dates))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(a.dates, key = { it.toString() }) { d ->
                        InputChip(
                            selected = false,
                            onClick = { alarm = a.copy(dates = a.dates - d) },
                            label = { Text(d.toString()) },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove_date, d.toString())) }
                        )
                    }
                }
                OutlinedButton(onClick = { showDatePicker = true }) { Text(stringResource(R.string.add_date)) }
            }
        }
    }

    if (showDatePicker) {
        val today = LocalDate.now()
        val pickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return date.isAfter(today)
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        alarm = a.copy(
                            repeatDays = 0,
                            dates = (a.dates + picked).distinct().sorted()
                        )
                    }
                    showDatePicker = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }
}
