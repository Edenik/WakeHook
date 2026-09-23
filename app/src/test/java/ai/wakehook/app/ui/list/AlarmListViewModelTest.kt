package ai.wakehook.app.ui.list

import androidx.test.core.app.ApplicationProvider
import ai.wakehook.app.alarm.AlarmScheduler
import ai.wakehook.app.data.AlarmDatabase
import ai.wakehook.app.data.RoomAlarmRepository
import ai.wakehook.app.domain.Alarm
import ai.wakehook.app.sync.FakeTombstoneStore
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
        val vm = AlarmListViewModel(repo, AlarmScheduler(app), FakeTombstoneStore())

        vm.toggle(Alarm(id = "a1", enabled = true))
        advanceUntilIdle()

        assertThat(repo.get("a1")!!.enabled).isFalse()
    }

    @Test fun delete_removesFromRepo() = runTest(dispatcher) {
        val repo = RoomAlarmRepository(AlarmDatabase.get(app).alarmDao())
        repo.upsert(Alarm(id = "a1"))
        val vm = AlarmListViewModel(repo, AlarmScheduler(app), FakeTombstoneStore())

        vm.delete("a1")
        advanceUntilIdle()

        assertThat(repo.get("a1")).isNull()
    }

    @Test fun delete_recordsTombstone() = runTest(dispatcher) {
        val repo = RoomAlarmRepository(AlarmDatabase.get(app).alarmDao())
        repo.upsert(Alarm(id = "a1"))
        val tombstones = FakeTombstoneStore()
        val vm = AlarmListViewModel(repo, AlarmScheduler(app), tombstones)

        vm.delete("a1")
        advanceUntilIdle()
        // repo.delete() hops onto Room's real (non-virtual-time) executor thread; awaiting a
        // suspend Room call here forces this test's coroutine to synchronize with that FIFO
        // executor, guaranteeing delete()'s later in-memory tombstones.add(id) has also run.
        assertThat(repo.get("a1")).isNull()

        assertThat(tombstones.local()).contains("a1")
    }
}
