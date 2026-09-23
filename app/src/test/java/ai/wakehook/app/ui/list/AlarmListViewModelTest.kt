package ai.wakehook.app.ui.list

import androidx.room.Room
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
    // Isolated in-memory DB per test — NOT the production AlarmDatabase.get() singleton,
    // whose Application-keyed cache is shared across test classes and causes cross-test flakes.
    private lateinit var db: AlarmDatabase
    private lateinit var repo: RoomAlarmRepository

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        db = Room.inMemoryDatabaseBuilder(app, AlarmDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = RoomAlarmRepository(db.alarmDao())
    }

    @After fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test fun toggle_flipsEnabledAndPersists() = runTest(dispatcher) {
        repo.upsert(Alarm(id = "a1", enabled = true))
        val vm = AlarmListViewModel(repo, AlarmScheduler(app), FakeTombstoneStore())

        vm.toggle(Alarm(id = "a1", enabled = true))
        advanceUntilIdle()

        assertThat(repo.get("a1")!!.enabled).isFalse()
    }

    @Test fun delete_removesFromRepo() = runTest(dispatcher) {
        repo.upsert(Alarm(id = "a1"))
        val vm = AlarmListViewModel(repo, AlarmScheduler(app), FakeTombstoneStore())

        vm.delete("a1")
        advanceUntilIdle()

        assertThat(repo.get("a1")).isNull()
    }

    @Test fun delete_recordsTombstone() = runTest(dispatcher) {
        repo.upsert(Alarm(id = "a1"))
        val tombstones = FakeTombstoneStore()
        val vm = AlarmListViewModel(repo, AlarmScheduler(app), tombstones)

        vm.delete("a1")
        advanceUntilIdle()

        assertThat(repo.get("a1")).isNull()
        assertThat(tombstones.local()).contains("a1")
    }
}
