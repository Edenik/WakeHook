package ai.wakehook.app.ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ai.wakehook.app.domain.Alarm
import ai.wakehook.app.domain.hasDay
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(vm: AlarmEditViewModel, alarmId: String?, onDone: () -> Unit) {
    var alarm by remember { mutableStateOf<Alarm?>(null) }
    LaunchedEffect(alarmId) { alarm = vm.load(alarmId) }
    val a = alarm ?: return

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (alarmId == null) "New alarm" else "Edit alarm") },
            actions = { TextButton(onClick = { vm.save(a); onDone() }) { Text("Save") } }
        )
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = a.hour.toString(),
                    onValueChange = { s -> s.toIntOrNull()?.takeIf { it in 0..23 }?.let { alarm = a.copy(hour = it) } },
                    label = { Text("Hour") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = a.minute.toString(),
                    onValueChange = { s -> s.toIntOrNull()?.takeIf { it in 0..59 }?.let { alarm = a.copy(minute = it) } },
                    label = { Text("Minute") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = a.label, onValueChange = { alarm = a.copy(label = it) },
                label = { Text("Label") }, modifier = Modifier.fillMaxWidth()
            )
            Text("Repeat")
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
    }
}
