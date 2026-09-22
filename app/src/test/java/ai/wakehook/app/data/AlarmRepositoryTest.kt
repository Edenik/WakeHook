package ai.wakehook.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ai.wakehook.app.domain.Alarm
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlarmRepositoryTest {
    private lateinit var db: AlarmDatabase
    private lateinit var repo: AlarmRepository

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AlarmDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = RoomAlarmRepository(db.alarmDao())
    }

    @After fun teardown() = db.close()

    @Test fun upsert_then_get_roundTrips() = runTest {
        val a = Alarm(id = "a1", label = "Gym", hour = 6, minute = 45, repeatDays = 0b0101010, enabled = true)
        repo.upsert(a)
        assertThat(repo.get("a1")).isEqualTo(a)
    }

    @Test fun upsert_sameId_updates_notDuplicates() = runTest {
        repo.upsert(Alarm(id = "a1", hour = 6, minute = 0))
        repo.upsert(Alarm(id = "a1", hour = 7, minute = 30))
        assertThat(repo.getAll()).hasSize(1)
        assertThat(repo.get("a1")!!.hour).isEqualTo(7)
    }

    @Test fun delete_removes() = runTest {
        repo.upsert(Alarm(id = "a1"))
        repo.delete("a1")
        assertThat(repo.get("a1")).isNull()
    }

    @Test fun observeAlarms_emitsCurrentList() = runTest {
        repo.upsert(Alarm(id = "a1"))
        assertThat(repo.observeAlarms().first().map { it.id }).containsExactly("a1")
    }
}
