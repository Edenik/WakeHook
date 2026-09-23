package ai.wakehook.app.ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.wakehook.app.R
import ai.wakehook.app.domain.Alarm
import ai.wakehook.app.domain.hasDay
import ai.wakehook.app.ui.theme.*
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle as DayTextStyle
import java.util.Locale

private enum class EditMode { ONCE, WEEKLY, DATES }
private fun modeOf(a: Alarm) = when { a.isDateBased -> EditMode.DATES; a.isRecurring -> EditMode.WEEKLY; else -> EditMode.ONCE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditScreen(vm: AlarmEditViewModel, alarmId: String?, onDone: () -> Unit) {
    var alarm by remember { mutableStateOf<Alarm?>(null) }
    LaunchedEffect(alarmId) { alarm = vm.load(alarmId) }
    val a = alarm ?: return
    var mode by remember(a.id) { mutableStateOf(modeOf(a)) }
    var hour by rememberSaveable(a.id) { mutableStateOf(a.hour.toString().padStart(2, '0')) }
    var minute by rememberSaveable(a.id) { mutableStateOf(a.minute.toString().padStart(2, '0')) }
    var showDatePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    val validTime = hour.toIntOrNull() in 0..23 && minute.toIntOrNull() in 0..59
    val validSchedule = when (mode) { EditMode.ONCE -> true; EditMode.WEEKLY -> a.repeatDays != 0; EditMode.DATES -> a.dates.isNotEmpty() }
    Scaffold(topBar = { ScreenHeader(stringResource(if (alarmId == null) R.string.new_alarm else R.string.edit_alarm), onDone) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = hour, onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) hour = it },
                        label = { Text(stringResource(R.string.hour)) }, singleLine = true,
                        isError = hour.toIntOrNull() !in 0..23,
                        textStyle = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    Text(":", fontSize = 36.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = minute, onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) minute = it },
                        label = { Text(stringResource(R.string.minute)) }, singleLine = true,
                        isError = minute.toIntOrNull() !in 0..59,
                        textStyle = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
            }
            Text(stringResource(R.string.time_format_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value = a.label, onValueChange = { alarm = a.copy(label = it) },
                label = { Text(stringResource(R.string.label)) }, modifier = Modifier.fillMaxWidth())
            SectionLabel(stringResource(R.string.mode))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(EditMode.ONCE to R.string.mode_once, EditMode.WEEKLY to R.string.mode_weekly, EditMode.DATES to R.string.mode_dates).forEach { (m, label) ->
                    FilterChip(selected = mode == m, onClick = {
                        mode = m
                        alarm = when (m) { EditMode.ONCE -> a.copy(repeatDays = 0, dates = emptyList()); EditMode.WEEKLY -> a.copy(dates = emptyList()); EditMode.DATES -> a.copy(repeatDays = 0) }
                    }, label = { Text(stringResource(label)) }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
            if (mode == EditMode.WEEKLY) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.values().forEach { day ->
                        FilterChip(selected = a.repeatDays.hasDay(day), onClick = { alarm = vm.toggleDay(a, day) },
                            label = { Text(day.getDisplayName(DayTextStyle.SHORT, Locale.getDefault())) }, modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
            }
            if (mode == EditMode.DATES) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    a.dates.forEach { d ->
                        InputChip(selected = false, onClick = { alarm = a.copy(dates = a.dates - d) },
                            label = { Text(d.toString()) }, trailingIcon = { Icon(Icons.Default.Close, stringResource(R.string.remove_date, d.toString())) },
                            modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
                OutlinedButton(shape = RoundedCornerShape(14.dp), onClick = { showDatePicker = true }) { Text(stringResource(R.string.add_date)) }
            }
            if (!validSchedule) Text(stringResource(R.string.choose_schedule), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Button(shape = RoundedCornerShape(14.dp), onClick = {
                saving = true
                scope.launch {
                    vm.save(a.copy(hour = hour.toInt(), minute = minute.toInt())).join()
                    onDone()
                }
            }, enabled = validTime && validSchedule && !saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.save)) }
            Text(stringResource(R.string.save_alarm_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                TextButton(shape = RoundedCornerShape(14.dp), onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        alarm = a.copy(
                            repeatDays = 0,
                            dates = (a.dates + picked).distinct().sorted()
                        )
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.add_date)) }
            },
            dismissButton = { TextButton(shape = RoundedCornerShape(14.dp), onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(state = pickerState) }
    }
}
