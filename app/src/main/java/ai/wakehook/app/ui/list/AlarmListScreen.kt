package ai.wakehook.app.ui.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import ai.wakehook.app.ui.theme.*
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter
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
        FloatingActionButton(onClick = onAdd, containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary) {
            Icon(Icons.Default.Add, stringResource(R.string.add_alarm))
        }
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 110.dp)) {
            item { SectionLabel(now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))) }
            if (!exactAlarmAllowed) item {
                DesignCard(Modifier.padding(vertical = 12.dp)) {
                    Text(stringResource(R.string.permission_attention), color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = onSettings) { Text(stringResource(R.string.check_permissions)) }
                }
            }
            item { NextAlarmCard(next, now, onEdit, Modifier.padding(top = 6.dp, bottom = 18.dp)) }
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
                AlarmListRow(a, onClick = { onEdit(a.id) }, onEnabledChange = { vm.toggle(a) })
            }
            item { Text(stringResource(R.string.local_alarm_hint), Modifier.padding(top = 20.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
