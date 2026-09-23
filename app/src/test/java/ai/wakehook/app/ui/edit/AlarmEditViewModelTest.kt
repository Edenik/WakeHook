package ai.wakehook.app.ui.edit

import androidx.test.core.app.ApplicationProvider
import ai.wakehook.app.alarm.AlarmScheduler
import ai.wakehook.app.data.AlarmDatabase
import ai.wakehook.app.data.RoomAlarmRepository
import ai.wakehook.app.domain.Alarm
import ai.wakehook.app.domain.hasDay
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek

@RunWith(RobolectricTestRunner::class)
class AlarmEditViewModelTest {
    private val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    private fun vm() = AlarmEditViewModel(
        RoomAlarmRepository(AlarmDatabase.get(app).alarmDao()), AlarmScheduler(app)
    )

    @Test fun toggleDay_setsAndClearsBit() {
        val v = vm()
        val withMon = v.toggleDay(Alarm(), DayOfWeek.MONDAY)
        assertThat(withMon.repeatDays.hasDay(DayOfWeek.MONDAY)).isTrue()
        val cleared = v.toggleDay(withMon, DayOfWeek.MONDAY)
        assertThat(cleared.repeatDays.hasDay(DayOfWeek.MONDAY)).isFalse()
    }

    @Test fun save_persistsAlarm() = runBlocking {
        val repo = RoomAlarmRepository(AlarmDatabase.get(app).alarmDao())
        val v = AlarmEditViewModel(repo, AlarmScheduler(app))
        val a = Alarm(id = "e1", label = "Gym", hour = 6, minute = 45)
        v.save(a); Thread.sleep(300)
        assertThat(repo.get("e1")).isEqualTo(a)
    }

    @Test fun load_null_returnsFreshAlarm() = runBlocking {
        assertThat(vm().load(null).id).isNotEmpty()
    }
}
