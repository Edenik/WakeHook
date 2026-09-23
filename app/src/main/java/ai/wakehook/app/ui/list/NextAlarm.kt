package ai.wakehook.app.ui.list

import ai.wakehook.app.domain.Alarm
import ai.wakehook.app.domain.AlarmTime
import java.time.ZonedDateTime

internal data class NextAlarm(val alarm: Alarm, val triggerAt: Long)

/** Uses the scheduler's recurrence rules, plus a pending snooze when it fires sooner. */
internal fun nextAlarm(alarms: List<Alarm>, now: ZonedDateTime): NextAlarm? = alarms
    .filter { it.enabled }
    .mapNotNull { alarm ->
        val regular = AlarmTime.nextTrigger(alarm, now)
        val snooze = alarm.snoozedUntil?.takeIf { it > now.toInstant().toEpochMilli() }
        listOfNotNull(regular, snooze).minOrNull()?.let { NextAlarm(alarm, it) }
    }.minByOrNull { it.triggerAt }
