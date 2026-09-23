package ai.wakehook.app.ui.list

import androidx.test.core.app.ApplicationProvider
import ai.wakehook.app.alarm.AlarmScheduler
import ai.wakehook.app.data.AlarmDatabase
import ai.wakehook.app.data.RoomAlarmRepository
import ai.wakehook.app.domain.Alarm
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AlarmListViewModelTest {
    private val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun toggle_flipsEnabledAndPersists() = runTest(dispatcher) {
        val repo = RoomAlarmRepository(AlarmDatabase.get(app).alarmDao())
        repo.upsert(Alarm(id = "a1", enabled = true))
        val vm = AlarmListViewModel(repo, AlarmScheduler(app))

        vm.toggle(Alarm(id = "a1", enabled = true))
        advanceUntilIdle()

        assertThat(repo.get("a1")!!.enabled).isFalse()
    }

    @Test fun delete_removesFromRepo() = runTest(dispatcher) {
        val repo = RoomAlarmRepository(AlarmDatabase.get(app).alarmDao())
        repo.upsert(Alarm(id = "a1"))
        val vm = AlarmListViewModel(repo, AlarmScheduler(app))

        vm.delete("a1")
        advanceUntilIdle()

        assertThat(repo.get("a1")).isNull()
    }
}
