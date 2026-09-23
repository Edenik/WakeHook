package ai.wakehook.app.ui.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.wakehook.app.R
import ai.wakehook.app.ui.theme.Sunrise
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Compact summary of the next scheduled alarm, matching the home mockup. */
@Composable
internal fun NextAlarmCard(next: NextAlarm?, now: ZonedDateTime, onEdit: (String) -> Unit,
    modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    val cardModifier = modifier.fillMaxWidth()
        .then(if (next != null) Modifier.clickable { onEdit(next.alarm.id) } else Modifier)
    Column(cardModifier.background(Sunrise, shape)
        .then(if (next != null) Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .22f)), shape) else Modifier)
        .padding(horizontal = 17.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.next_alarm), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary, letterSpacing = 1.3.sp)
            if (next != null) Text(relativeTime(next.triggerAt, now), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (next == null) {
            Text(stringResource(R.string.no_active_alarms), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.enable_alarm_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val trigger = Instant.ofEpochMilli(next.triggerAt).atZone(now.zone)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(trigger.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)),
                    fontSize = 43.sp, lineHeight = 48.sp, fontWeight = FontWeight.Light,
                    letterSpacing = (-1.8).sp, color = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(next.alarm.label.ifBlank { stringResource(R.string.alarms) },
                        style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Text(trigger.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun relativeTime(triggerAt: Long, now: ZonedDateTime): String {
    val minutes = Duration.between(now.toInstant(), Instant.ofEpochMilli(triggerAt)).toMinutes().coerceAtLeast(0)
    val hours = minutes / 60
    val remaining = minutes % 60
    return if (hours == 0L) stringResource(R.string.alarm_in_minutes, minutes)
    else stringResource(R.string.alarm_in_hours_minutes, hours, remaining)
}
