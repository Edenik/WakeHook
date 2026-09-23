package ai.wakehook.app.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class AlarmDateRangesTest {
    @Test fun groupsConsecutiveDaysAndKeepsSeparateRanges() {
        val first = LocalDate.of(2026, 9, 25)
        val second = LocalDate.of(2026, 10, 3)
        assertThat(groupAlarmDates(listOf(first.plusDays(1), second, first, second.plusDays(2))))
            .containsExactly(
                AlarmDateRange(first, first.plusDays(1)),
                AlarmDateRange(second, second),
                AlarmDateRange(second.plusDays(2), second.plusDays(2)),
            ).inOrder()
    }

    @Test fun rangeExpandsBothEndpointsInclusively() {
        val start = LocalDate.of(2026, 9, 25)
        assertThat(AlarmDateRange(start, start.plusDays(2)).dates())
            .containsExactly(start, start.plusDays(1), start.plusDays(2)).inOrder()
    }
}
