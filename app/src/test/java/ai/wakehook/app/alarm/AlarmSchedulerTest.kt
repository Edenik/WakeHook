package ai.wakehook.app.alarm

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import ai.wakehook.app.domain.Alarm
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AlarmSchedulerTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingBroadcast(id: String): PendingIntent? = PendingIntent.getBroadcast(
        app, AlarmIntents.requestCode(id),
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
}
