package ai.wakehook.app.ui.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.wakehook.app.R
import ai.wakehook.app.domain.Alarm
import ai.wakehook.app.ui.utils.dateScheduleLabel
import ai.wakehook.app.ui.utils.weeklyScheduleLabel
import java.util.Locale

/** Compact, single-line alarm list item with a separately operable enable switch. */
@Composable
fun AlarmListRow(
    alarm: Alarm,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 79.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.weight(1f).heightIn(min = 64.dp),
            contentPadding = PaddingValues(vertical = 10.dp),
            shape = RoundedCornerShape(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("%02d:%02d".format(Locale.ROOT, alarm.hour, alarm.minute),
                    modifier = Modifier.width(81.dp), fontSize = 31.sp, lineHeight = 36.sp,
                    fontWeight = FontWeight.Light, letterSpacing = (-1.1).sp,
                    color = if (alarm.enabled) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.Start) {
                    Text(alarm.label.ifBlank { stringResource(R.string.alarms) },
                        color = if (alarm.enabled) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp, lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (alarm.ackState == "error") stringResource(R.string.schedule_error) else scheduleLabel(alarm),
                        color = if (alarm.ackState == "error") MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (alarm.source != "local" && alarm.ackState != "error") {
                        Text("✳ ${sourceLabel(alarm.source)}", color = MaterialTheme.colorScheme.secondary,
                            fontSize = 9.sp, lineHeight = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Switch(checked = alarm.enabled, onCheckedChange = onEnabledChange)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))
}

@Composable
private fun sourceLabel(source: String): String = when (source) {
    "wakehook" -> stringResource(R.string.example_alarm)
    else -> stringResource(R.string.set_by_agent, source)
}

@Composable
private fun scheduleLabel(alarm: Alarm): String {
    val locale = Locale.getDefault()
    if (alarm.isDateBased) return dateScheduleLabel(alarm.dates, locale)
    if (!alarm.isRecurring) return stringResource(R.string.mode_once)
    return weeklyScheduleLabel(alarm.repeatDays, locale)
}
