package ai.wakehook.app.sync

import ai.wakehook.app.domain.Alarm
import ai.wakehook.app.domain.dayBit
import java.time.DayOfWeek

/**
 * The example content written into a brand-new `WakeHook/` folder on first connect: three
 * disabled sample alarms (so nothing rings unexpectedly) plus a short guide for an AI agent
 * reading/writing `wakehook.json` directly.
 */
object Seed {
    private val WEEKDAYS = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
    ).fold(0) { acc, day -> acc or dayBit(day) }

    private val EVERY_DAY = DayOfWeek.values().fold(0) { acc, day -> acc or dayBit(day) }

    fun exampleAlarms(): List<Alarm> = listOf(
        Alarm(
            id = "seed-wake-up",
            label = "Wake up (example)",
            hour = 7,
            minute = 0,
            repeatDays = 0,
            enabled = false,
            source = "wakehook",
        ),
        Alarm(
            id = "seed-standup",
            label = "Standup (example)",
            hour = 8,
            minute = 30,
            repeatDays = WEEKDAYS,
            enabled = false,
            source = "wakehook",
        ),
        Alarm(
            id = "seed-wind-down",
            label = "Wind down (example)",
            hour = 22,
            minute = 0,
            repeatDays = EVERY_DAY,
            enabled = false,
            source = "wakehook",
        ),
    )

    val MARKDOWN: String = """
        # WakeHook agent guide

        This folder (`WakeHook/wakehook.json`) is how an AI agent controls alarms in the
        WakeHook app. The app reads it on every sync and writes back an `ack` per alarm.

        ## File

        `WakeHook/wakehook.json` — self-describing; see its own `instructions`/`example` fields.
        Each alarm has: `id` (unique string), `hour` (0-23), `minute` (0-59), `label`,
        `daysOfWeek` (list of 0=Sun..6=Sat, for weekly repeats), `dates` (list of `yyyy-MM-dd`
        for one-off dates), `enabled` (bool), `source` (who created it), `version` (bump on
        every edit), and an app-written `ack` (`state`/`at`/`error`).

        ## How to

        - **Add**: append a new object to `alarms` with a fresh `id` and `enabled: true`.
        - **Edit**: change fields on an existing alarm and increment its `version`.
        - **Cancel**: either set `enabled: false`, or delete its object from `alarms` entirely.
        - **List**: read the `alarms` array; each entry's `ack.state` shows whether the app
          has scheduled it, disabled it, or hit an error.

        The three example alarms above are disabled by default — enable one or add your own.
    """.trimIndent()
}
