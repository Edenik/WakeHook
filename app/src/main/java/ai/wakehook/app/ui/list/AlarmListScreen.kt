package ai.wakehook.app.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.wakehook.app.R
import ai.wakehook.app.domain.Alarm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    vm: AlarmListViewModel,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onSettings: () -> Unit = {},
) {
    val alarms by vm.alarms.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Icon(Icons.Filled.Add, stringResource(R.string.add_alarm)) }
        }
    ) { padding ->
        if (alarms.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.empty_alarms),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(alarms, key = { it.id }) { a ->
                    AlarmRow(a, onToggle = { vm.toggle(a) }, onClick = { onEdit(a.id) })
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(a: Alarm, onToggle: () -> Unit, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text("%02d:%02d".format(a.hour, a.minute)) },
        supportingContent = { if (a.label.isNotEmpty()) Text(a.label) },
        trailingContent = { Switch(checked = a.enabled, onCheckedChange = { onToggle() }) },
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 8.dp)
    )
}
