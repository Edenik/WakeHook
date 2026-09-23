package ai.wakehook.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ai.wakehook.app.R

/** A setting with an adjustable value and a separate on/off control. */
@Composable
fun PlaybackPreferenceRow(
    title: String,
    value: String,
    enabled: Boolean,
    offLabel: String,
    onValueClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    valueMenu: (@Composable () -> Unit)? = null,
) {
    Box(modifier) {
        Row(Modifier.fillMaxWidth().heightIn(min = 72.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onValueClick, modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 5.dp)) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.Start) {
                    Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                    Text(if (enabled) value else offLabel, color = if (enabled) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            Switch(checked = enabled, onCheckedChange = onEnabledChange,
                modifier = Modifier.semantics { contentDescription = title })
        }
        valueMenu?.let { menu -> Box(Modifier.align(Alignment.CenterStart).padding(end = 74.dp)) { menu() } }
    }
}
