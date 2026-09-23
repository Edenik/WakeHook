package ai.wakehook.app.alarm

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import ai.wakehook.app.data.AlarmDatabase
import ai.wakehook.app.data.RoomAlarmRepository
import ai.wakehook.app.data.toEntity
import ai.wakehook.app.domain.Alarm
import ai.wakehook.app.domain.dayBit
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek

@RunWith(RobolectricTestRunner::class)
class AlarmReceiverTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun pending(id: String): PendingIntent? = PendingIntent.getBroadcast(
        app, AlarmIntents.requestCode(id),
        Intent(app, AlarmReceiver::class.java),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )

    @Test fun recurringAlarm_isRearmedOnReceive() = runBlocking {
        val db = AlarmDatabase.get(app)
        val repo = RoomAlarmRepository(db.alarmDao())
        val everyDay = (0..6).fold(0) { acc, d -> acc or dayBit(DayOfWeek.of(if (d == 0) 7 else d)) }
        repo.upsert(Alarm(id = "r1", hour = 6, minute = 0, repeatDays = everyDay))

        val intent = Intent(app, AlarmReceiver::class.java)
            .putExtra(AlarmIntents.EXTRA_ID, "r1")
            .putExtra(AlarmIntents.EXTRA_LABEL, "")
            .putExtra(AlarmIntents.EXTRA_SNOOZE, false)
        AlarmReceiver().onReceive(app, intent)

        assertThat(pending("r1")).isNotNull()
    }

    @Test fun oneTimeAlarm_isDisabledAfterFiring() = runBlocking {
        val db = AlarmDatabase.get(app)
        val repo = RoomAlarmRepository(db.alarmDao())
        repo.upsert(Alarm(id = "o1", hour = 6, minute = 0, repeatDays = 0, enabled = true))

        val intent = Intent(app, AlarmReceiver::class.java)
            .putExtra(AlarmIntents.EXTRA_ID, "o1")
            .putExtra(AlarmIntents.EXTRA_LABEL, "")
            .putExtra(AlarmIntents.EXTRA_SNOOZE, false)
        AlarmReceiver().onReceive(app, intent)

        val stored = repo.get("o1")
        assertThat(stored).isNotNull()
        assertThat(stored!!.enabled).isFalse()
    }
}
