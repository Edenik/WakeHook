package ai.wakehook.app.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmTimeTest {
    private val zone = ZoneId.of("Asia/Jerusalem")
    // Wed 2026-09-23 08:00 local
    private val now = ZonedDateTime.of(2026, 9, 23, 8, 0, 0, 0, zone)

    private fun alarm(hour: Int, minute: Int, repeatDays: Int = 0, enabled: Boolean = true) =
        Alarm("id", "", hour, minute, repeatDays, enabled)

    private fun expected(y: Int, mo: Int, d: Int, h: Int, mi: Int) =
        ZonedDateTime.of(y, mo, d, h, mi, 0, 0, zone).toInstant().toEpochMilli()

    @Test fun disabled_returns_null() {
        assertThat(AlarmTime.nextTrigger(alarm(9, 0, enabled = false), now)).isNull()
    }

    @Test fun oneTime_laterToday() {
        assertThat(AlarmTime.nextTrigger(alarm(9, 0), now))
            .isEqualTo(expected(2026, 9, 23, 9, 0))
    }

    @Test fun oneTime_earlierToday_rollsToTomorrow() {
        assertThat(AlarmTime.nextTrigger(alarm(7, 0), now))
            .isEqualTo(expected(2026, 9, 24, 7, 0))
    }

    @Test fun oneTime_exactlyNow_rollsToTomorrow() {
        assertThat(AlarmTime.nextTrigger(alarm(8, 0), now))
            .isEqualTo(expected(2026, 9, 24, 8, 0))
    }

    @Test fun recurring_todayLater_firesToday() {
        val wed = dayBit(DayOfWeek.WEDNESDAY)
        assertThat(AlarmTime.nextTrigger(alarm(9, 0, wed), now))
            .isEqualTo(expected(2026, 9, 23, 9, 0))
    }

    @Test fun recurring_todayPassed_firesNextWeek() {
        val wed = dayBit(DayOfWeek.WEDNESDAY)
        assertThat(AlarmTime.nextTrigger(alarm(7, 0, wed), now))
            .isEqualTo(expected(2026, 9, 30, 7, 0))
    }

    @Test fun recurring_picksNearestUpcomingDay() {
        val friSun = dayBit(DayOfWeek.FRIDAY) or dayBit(DayOfWeek.SUNDAY)
        // From Wed, nearest is Fri 2026-09-25
        assertThat(AlarmTime.nextTrigger(alarm(6, 30, friSun), now))
            .isEqualTo(expected(2026, 9, 25, 6, 30))
    }

    @Test fun dst_springForward_isValidInstant() {
        // Israel DST 2026 begins Fri 2026-03-27 02:00 -> 03:00.
        val z = ZoneId.of("Asia/Jerusalem")
        val before = ZonedDateTime.of(2026, 3, 27, 1, 0, 0, 0, z)
        val result = AlarmTime.nextTrigger(alarm(2, 30), before)
        // 02:30 does not exist; java.time shifts it forward. Assert it is after 'before'.
        assertThat(result).isNotNull()
        assertThat(result!!).isGreaterThan(before.toInstant().toEpochMilli())
    }
}
