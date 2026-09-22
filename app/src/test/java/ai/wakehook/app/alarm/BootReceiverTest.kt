package ai.wakehook.app.alarm

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import ai.wakehook.app.data.AlarmDatabase
import ai.wakehook.app.data.RoomAlarmRepository
import ai.wakehook.app.domain.Alarm
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BootReceiverTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun pending(id: String): PendingIntent? = PendingIntent.getBroadcast(
        app, AlarmIntents.requestCode(id),
        Intent(app, AlarmReceiver::class.java),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )

    @Test fun boot_reschedulesEnabledAlarms() = runBlocking {
        val repo = RoomAlarmRepository(AlarmDatabase.get(app).alarmDao())
        repo.upsert(Alarm(id = "b1", hour = 23, minute = 30, enabled = true))

        BootReceiver().onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))
        Thread.sleep(500) // allow the async coroutine to finish

        assertThat(pending("b1")).isNotNull()
    }
}
