package ai.wakehook.app.domain

import java.time.DayOfWeek
import java.time.ZonedDateTime

/** Bit index for a day: Sun=0, Mon=1, ... Sat=6. */
fun dayBit(day: DayOfWeek): Int = 1 shl (day.value % 7)

fun Int.hasDay(day: DayOfWeek): Boolean = (this and dayBit(day)) != 0

object AlarmTime {
    /** Next fire time in epoch millis, or null if none (disabled). */
    fun nextTrigger(alarm: Alarm, now: ZonedDateTime): Long? {
        if (!alarm.enabled) return null
        var candidate = now
            .withHour(alarm.hour).withMinute(alarm.minute)
            .withSecond(0).withNano(0)

        if (!alarm.isRecurring) {
            if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
            return candidate.toInstant().toEpochMilli()
        }
        // Recurring: scan up to 8 days for the nearest matching, future day.
        for (i in 0..7) {
            if (candidate.isAfter(now) && alarm.repeatDays.hasDay(candidate.dayOfWeek)) {
                return candidate.toInstant().toEpochMilli()
            }
            candidate = candidate.plusDays(1)
        }
        return null
    }
}
