package ai.wakehook.app.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ai.wakehook.app.R
import ai.wakehook.app.domain.AlarmDateRange
import ai.wakehook.app.domain.groupAlarmDates
import ai.wakehook.app.domain.hasDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val weekDays = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)

/** Shared recurrence label used by the alarm editor and compact home rows. */
@Composable
fun weeklyScheduleLabel(repeatDays: Int, locale: Locale = Locale.getDefault()): String {
    val selected = weekDays.filter { repeatDays.hasDay(it) }
    return when {
        selected.size == 7 -> stringResource(R.string.schedule_every_day)
        selected == weekDays.subList(1, 6) -> stringResource(R.string.schedule_weekdays)
        selected == listOf(DayOfWeek.SUNDAY, DayOfWeek.SATURDAY) -> stringResource(R.string.schedule_weekends)
        else -> selected.joinToString(" · ") { it.getDisplayName(TextStyle.SHORT, locale) }
    }
}

/** Shared date-based schedule summary; consecutive days are displayed as one range. */
@Composable
fun dateScheduleLabel(dates: Collection<LocalDate>, locale: Locale = Locale.getDefault()): String {
    val ranges = groupAlarmDates(dates)
    return when {
        ranges.size > 1 -> stringResource(R.string.date_ranges_count, ranges.size)
        ranges.size == 1 -> ranges.single().displayLabel(locale)
        else -> stringResource(R.string.add_date_range_hint)
    }
}

fun AlarmDateRange.displayLabel(locale: Locale = Locale.getDefault()): String = when {
    start == endInclusive -> start.format(DateTimeFormatter.ofPattern("EEE, MMM d", locale))
    start.year == endInclusive.year -> "${start.format(DateTimeFormatter.ofPattern("MMM d", locale))} – ${endInclusive.format(DateTimeFormatter.ofPattern("MMM d", locale))}"
    else -> "${start.format(DateTimeFormatter.ofPattern("MMM d, yyyy", locale))} – ${endInclusive.format(DateTimeFormatter.ofPattern("MMM d, yyyy", locale))}"
}
