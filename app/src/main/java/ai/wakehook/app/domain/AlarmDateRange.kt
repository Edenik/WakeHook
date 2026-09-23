package ai.wakehook.app.domain

import java.time.LocalDate

/** A consecutive inclusive range of days in an alarm's date-based schedule. */
data class AlarmDateRange(val start: LocalDate, val endInclusive: LocalDate) {
    init { require(!endInclusive.isBefore(start)) }

    fun dates(): List<LocalDate> = generateSequence(start) { date ->
        date.plusDays(1).takeUnless { it.isAfter(endInclusive) }
    }.toList()
}

/** Groups a sorted set of dates into consecutive ranges for compact display and removal. */
fun groupAlarmDates(dates: Collection<LocalDate>): List<AlarmDateRange> {
    val sorted = dates.distinct().sorted()
    if (sorted.isEmpty()) return emptyList()
    val ranges = mutableListOf<AlarmDateRange>()
    var start = sorted.first()
    var end = start
    for (date in sorted.drop(1)) {
        if (date == end.plusDays(1)) end = date
        else {
            ranges += AlarmDateRange(start, end)
            start = date
            end = date
        }
    }
    ranges += AlarmDateRange(start, end)
    return ranges
}
