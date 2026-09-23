package ai.wakehook.app.alarm

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import ai.wakehook.app.domain.Alarm
import ai.wakehook.app.domain.dayBit
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.DayOfWeek

@RunWith(RobolectricTestRunner::class)
class AlarmSchedulerTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingBroadcast(id: String): PendingIntent? = PendingIntent.getBroadcast(
        app, AlarmIntents.requestCode(id),
        Intent(app, AlarmReceiver::class.java),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )

    private fun pendingSnoozeBroadcast(id: String): PendingIntent? = PendingIntent.getBroadcast(
        app, AlarmIntents.snoozeRequestCode(id),
        Intent(app, AlarmReceiver::class.java),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )

    @Test fun schedule_registersAnAlarm() {
        AlarmScheduler(app).schedule(Alarm(id = "a1", hour = 23, minute = 59))
        assertThat(shadowOf(am).nextScheduledAlarm).isNotNull()
    }

    @Test fun cancel_clearsPendingIntent() {
        val s = AlarmScheduler(app)
        s.schedule(Alarm(id = "a1", hour = 23, minute = 59))
        assertThat(pendingBroadcast("a1")).isNotNull()
        s.cancel("a1")
        assertThat(pendingBroadcast("a1")).isNull()
    }

    @Test fun disabledAlarm_isNotScheduled() {
        AlarmScheduler(app).schedule(Alarm(id = "a1", enabled = false))
        assertThat(pendingBroadcast("a1")).isNull()
    }

    @Test fun scheduleSnooze_doesNotClobberRecurringAlarmsRearmedOccurrence() {
        val s = AlarmScheduler(app)
        val everyDay = (0..6).fold(0) { acc, d -> acc or dayBit(DayOfWeek.of(if (d == 0) 7 else d)) }
        // Simulates AlarmReceiver re-arming the next occurrence of a recurring alarm
        // (registered under requestCode(id)) before the user taps Snooze on the current fire.
        s.schedule(Alarm(id = "r1", hour = 6, minute = 0, repeatDays = everyDay))
        assertThat(pendingBroadcast("r1")).isNotNull()

        s.scheduleSnooze("r1", "label", System.currentTimeMillis() + 10 * 60 * 1000L)

        assertThat(pendingBroadcast("r1")).isNotNull()
        assertThat(pendingSnoozeBroadcast("r1")).isNotNull()
    }
}
