package ai.wakehook.app.sync

import ai.wakehook.app.domain.Alarm
import ai.wakehook.app.domain.dayBit
import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WakeHookFileTest {

    @Test fun roundTrip_weekly_dates_and_agentWithAck() {
        val weekly = Alarm(
            id = "weekly-1",
            label = "Gym",
            hour = 6,
            minute = 45,
            repeatDays = dayBit(DayOfWeek.MONDAY) or dayBit(DayOfWeek.WEDNESDAY),
            enabled = true,
            source = "local",
            version = 2,
        )
        val dated = Alarm(
            id = "dated-1",
            label = "Flight",
            hour = 4,
            minute = 30,
            dates = listOf(LocalDate.of(2026, 12, 25), LocalDate.of(2027, 1, 1)),
            enabled = true,
            source = "local",
            version = 1,
        )
        val agent = Alarm(
            id = "agent-1",
            label = "Standup",
            hour = 9,
            minute = 0,
            repeatDays = dayBit(DayOfWeek.TUESDAY),
            enabled = true,
            source = "claude",
            version = 3,
            ackState = "scheduled",
            ackAt = "2026-09-23T08:00:00Z",
            ackError = "",
            snoozedUntil = 123456789L,
        )

        val json = WakeHookJson.encode(listOf(weekly, dated, agent))
        val decoded = WakeHookJson.decode(json)

        assertThat(decoded.alarms).containsExactly(weekly, dated, agent)
    }

    @Test fun decode_minimal_withId_yieldsDefaults() {
        val decoded = WakeHookJson.decode("""{"alarms":[{"id":"m1","hour":6,"minute":45}]}""")

        assertThat(decoded.alarms).hasSize(1)
        val a = decoded.alarms[0]
        assertThat(a.id).isEqualTo("m1")
        assertThat(a.hour).isEqualTo(6)
        assertThat(a.minute).isEqualTo(45)
        assertThat(a.label).isEqualTo("")
        assertThat(a.repeatDays).isEqualTo(0)
        assertThat(a.dates).isEmpty()
        assertThat(a.enabled).isTrue()
        assertThat(a.source).isEqualTo("local")
        assertThat(a.version).isEqualTo(0)
        assertThat(a.ackState).isEqualTo("")
        assertThat(a.snoozedUntil).isNull()
    }

    // Was `decode_minimal_yieldsDefaults`, and previously asserted that an id-less entry decoded
    // to a fabricated random-UUID alarm -- that encoded the bug this test now guards against
    // (see decodeAlarm): an id-less/blank-id entry must be DROPPED, never given a synthesized id.
    @Test fun decode_entryWithNoId_isDropped_notFabricated() {
        val decoded = WakeHookJson.decode("""{"alarms":[{"hour":6,"minute":45}]}""")

        assertThat(decoded.alarms).isEmpty()
    }

    @Test fun decode_mixOfIdLessAndValidEntries_dropsOnlyIdLess() {
        val json = """
            {"alarms":[
                {"hour":6,"minute":45},
                {"id":"valid-1","hour":8,"minute":15},
                {"id":"","hour":9,"minute":0}
            ]}
        """.trimIndent()

        val decoded = WakeHookJson.decode(json)

        assertThat(decoded.alarms.map { it.id }).containsExactly("valid-1")
    }

    @Test fun decode_empty_yieldsEmptyAlarms() {
        val decoded = WakeHookJson.decode("{}")

        assertThat(decoded.alarms).isEmpty()
    }

    @Test fun encode_containsInstructionsAndExample() {
        val json = WakeHookJson.encode(emptyList())

        assertThat(json).contains("instructions")
        assertThat(json).contains("example")
    }

    @Test fun repeatDaysToInts_and_intsToRepeatDays_roundTrip() {
        val mask = dayBit(DayOfWeek.SUNDAY) or dayBit(DayOfWeek.SATURDAY)
        val ints = repeatDaysToInts(mask)

        assertThat(ints).containsExactly(0, 6)
        assertThat(intsToRepeatDays(ints)).isEqualTo(mask)
    }
}
