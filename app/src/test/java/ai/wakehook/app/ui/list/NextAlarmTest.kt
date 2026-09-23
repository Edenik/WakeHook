package ai.wakehook.app.ui.list

import ai.wakehook.app.domain.Alarm
import ai.wakehook.app.domain.dayBit
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZonedDateTime

class NextAlarmTest {
    private val now = ZonedDateTime.parse("2026-09-23T22:14:00+03:00[Asia/Jerusalem]")

    @Test fun disabledAlarmDoesNotExposeCancelledSnooze() {
        val alarm = Alarm(enabled = false, snoozedUntil = now.plusMinutes(10).toInstant().toEpochMilli())
        assertNull(nextAlarm(listOf(alarm), now))
    }

    @Test fun snoozePrecedesNextRegularOccurrence() {
        val at = now.plusMinutes(10).toInstant().toEpochMilli()
        val alarm = Alarm(hour = 7, snoozedUntil = at)
        assertEquals(at, nextAlarm(listOf(alarm), now)?.triggerAt)
    }

    @Test fun expiredSnoozeIsIgnored() {
        val alarm = Alarm(hour = 7, snoozedUntil = now.minusMinutes(10).toInstant().toEpochMilli())
        assertEquals(now.plusDays(1).withHour(7).withMinute(0).toInstant().toEpochMilli(), nextAlarm(listOf(alarm), now)?.triggerAt)
    }

    @Test fun selectsActualDateRatherThanSmallestClockTime() {
        val tomorrow = Alarm(id = "tomorrow", hour = 9, minute = 0, dates = listOf(now.toLocalDate().plusDays(1)))
        val later = Alarm(id = "later", hour = 6, minute = 0, repeatDays = dayBit(DayOfWeek.FRIDAY))
        assertEquals("tomorrow", nextAlarm(listOf(later, tomorrow), now)?.alarm?.id)
    }

    @Test fun exhaustedDatesDoNotAppearAsUpcoming() {
        assertNull(nextAlarm(listOf(Alarm(dates = listOf(now.toLocalDate().minusDays(1)))), now))
    }
}
