package ai.wakehook.app.ui.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ai.wakehook.app.R
import ai.wakehook.app.domain.AlarmDateRange
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val calendarWeekdays = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)

/** Compact calendar for adding individual future dates and multiple inclusive ranges. */
@Composable
fun AlarmDatePicker(
    selectedDates: List<LocalDate>,
    onDismiss: () -> Unit,
    onAddDates: (List<AlarmDateRange>) -> Unit,
) {
    val today = LocalDate.now()
    val firstMonth = YearMonth.from(today)
    val lastMonth = firstMonth.plusYears(10)
    var displayedMonthIndex by rememberSaveable {
        mutableIntStateOf(firstMonth.year * 12 + firstMonth.monthValue - 1)
    }
    val displayedMonth = YearMonth.of(displayedMonthIndex / 12, displayedMonthIndex % 12 + 1)
    var selectionMode by rememberSaveable { mutableIntStateOf(SPECIFIC_DATES_MODE) }
    var rangeStartMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var rangeEndMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var multipleDateMillis by rememberSaveable { mutableStateOf<List<Long>>(emptyList()) }
    val locale = Locale.getDefault()
    val rangeStart = rangeStartMillis?.toLocalDate()
    val rangeEnd = rangeEndMillis?.toLocalDate()
    val savedDateSet = selectedDates.toSet()
    val hasSelection = when (selectionMode) {
        DATE_RANGE_MODE -> rangeStart != null && rangeEnd != null
        else -> multipleDateMillis.isNotEmpty()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.select_dates_title), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectionMode == SPECIFIC_DATES_MODE,
                        onClick = {
                            selectionMode = SPECIFIC_DATES_MODE
                            rangeStartMillis = null
                            rangeEndMillis = null
                            multipleDateMillis = emptyList()
                        },
                        label = { Text(stringResource(R.string.specific_dates), maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                    )
                    FilterChip(
                        selected = selectionMode == DATE_RANGE_MODE,
                        onClick = {
                            selectionMode = DATE_RANGE_MODE
                            rangeStartMillis = null
                            rangeEndMillis = null
                            multipleDateMillis = emptyList()
                        },
                        label = { Text(stringResource(R.string.date_range), maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                    )
                }
                Text(
                    stringResource(when {
                        selectionMode == DATE_RANGE_MODE && rangeStart == null -> R.string.choose_start_date
                        selectionMode == DATE_RANGE_MODE && rangeEnd == null -> R.string.choose_end_date
                        selectionMode == DATE_RANGE_MODE -> R.string.date_range_ready
                        else -> R.string.choose_multiple_dates
                    }),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(
                        enabled = displayedMonth > firstMonth,
                        onClick = { displayedMonthIndex-- },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.previous_month))
                    }
                    Text(displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
                        style = MaterialTheme.typography.titleMedium)
                    IconButton(
                        enabled = displayedMonth < lastMonth,
                        onClick = { displayedMonthIndex++ },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.next_month))
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    calendarWeekdays.forEach { day ->
                        Box(Modifier.weight(1f).height(32.dp), contentAlignment = Alignment.Center) {
                            Text(day.getDisplayName(TextStyle.NARROW, locale),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                val leadingDays = displayedMonth.atDay(1).dayOfWeek.value % 7
                val dayCells = (0 until 42).map { index ->
                    (index - leadingDays + 1).takeIf { it in 1..displayedMonth.lengthOfMonth() }
                        ?.let(displayedMonth::atDay)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    dayCells.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth()) {
                            week.forEach { date ->
                                CalendarDateCell(
                                    date = date,
                                    enabled = date != null && date.isAfter(today),
                                    selected = date != null && when (selectionMode) {
                                        DATE_RANGE_MODE -> date == rangeStart || date == rangeEnd ||
                                            (date in savedDateSet && date.minusDays(1) !in savedDateSet &&
                                                date.plusDays(1) !in savedDateSet)
                                        else -> date in savedDateSet || date.toUtcMillis() in multipleDateMillis
                                    },
                                    inRange = date != null && (
                                        selectionMode == DATE_RANGE_MODE && rangeStart != null && rangeEnd != null &&
                                            date.isAfter(rangeStart) && date.isBefore(rangeEnd) ||
                                            date in savedDateSet && (date.minusDays(1) in savedDateSet ||
                                                date.plusDays(1) in savedDateSet)),
                                    onClick = {
                                        if (date != null && date.isAfter(today)) {
                                            displayedMonthIndex = date.year * 12 + date.monthValue - 1
                                            if (selectionMode == DATE_RANGE_MODE &&
                                                (rangeStart == null || rangeEnd != null || date.isBefore(rangeStart))) {
                                                rangeStartMillis = date.toUtcMillis()
                                                rangeEndMillis = null
                                            } else if (selectionMode == DATE_RANGE_MODE) {
                                                rangeEndMillis = date.toUtcMillis()
                                            } else {
                                                val millis = date.toUtcMillis()
                                                if (date !in savedDateSet) multipleDateMillis = if (millis in multipleDateMillis)
                                                    multipleDateMillis - millis else (multipleDateMillis + millis).sorted()
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
                    Button(
                        enabled = hasSelection,
                        onClick = {
                            val addedDates = when (selectionMode) {
                                DATE_RANGE_MODE -> if (rangeStart != null && rangeEnd != null) {
                                    val range = AlarmDateRange(rangeStart, rangeEnd)
                                    rangeStartMillis = null
                                    rangeEndMillis = null
                                    listOf(range)
                                } else emptyList()
                                else -> multipleDateMillis.map { it.toLocalDate() }
                                    .map { AlarmDateRange(it, it) }
                            }
                            if (addedDates.isNotEmpty()) {
                                onAddDates(addedDates)
                                multipleDateMillis = emptyList()
                            }
                        },
                    ) {
                        Text(stringResource(if (selectionMode == DATE_RANGE_MODE)
                            R.string.add_date_range else R.string.add_multiple_dates))
                    }
                }
            }
        }
    }
}

private const val SPECIFIC_DATES_MODE = 0
private const val DATE_RANGE_MODE = 1

@Composable
private fun RowScope.CalendarDateCell(
    date: LocalDate?,
    enabled: Boolean,
    selected: Boolean,
    inRange: Boolean,
    onClick: () -> Unit,
) {
    val background = when {
        selected -> MaterialTheme.colorScheme.primary
        inRange -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surface
    }
    val foreground = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }
    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
        if (date != null) {
            Surface(
                modifier = Modifier.fillMaxSize()
                    .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
                    .semantics { this.selected = selected },
                shape = CircleShape,
                color = background,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium, color = foreground)
                }
            }
        }
    }
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
