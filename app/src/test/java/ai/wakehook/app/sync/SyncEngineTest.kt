package ai.wakehook.app.sync

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ai.wakehook.app.alarm.AlarmIntents
import ai.wakehook.app.alarm.AlarmReceiver
import ai.wakehook.app.alarm.AlarmScheduler
import ai.wakehook.app.data.AlarmDatabase
import ai.wakehook.app.data.AlarmRepository
import ai.wakehook.app.data.RoomAlarmRepository
import ai.wakehook.app.domain.Alarm
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncEngineTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private lateinit var db: AlarmDatabase
    private lateinit var repo: AlarmRepository
    private lateinit var provider: FakeSyncProvider
    private lateinit var scheduler: AlarmScheduler
    private lateinit var tombstones: FakeTombstoneStore
    private lateinit var notifier: FakeAgentNotifier
    private lateinit var state: SyncState
    private lateinit var engine: SyncEngine

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(app, AlarmDatabase::class.java).allowMainThreadQueries().build()
        repo = RoomAlarmRepository(db.alarmDao())
        provider = FakeSyncProvider()
        scheduler = AlarmScheduler(app)
        tombstones = FakeTombstoneStore()
        notifier = FakeAgentNotifier()
        state = SyncState(app)
        engine = SyncEngine(provider, repo, scheduler, tombstones, notifier, state)
    }

    @After fun teardown() = db.close()

    private fun pendingBroadcast(id: String): PendingIntent? = PendingIntent.getBroadcast(
        app, AlarmIntents.requestCode(id),
        Intent(app, AlarmReceiver::class.java),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )

    @Test fun remoteNewEnabledAgentAlarm_isAdopted_scheduled_acked_andNotified() = runTest {
        val remoteAlarm = Alarm(
            id = "r1", label = "Standup", hour = 9, minute = 0,
            enabled = true, source = "claude", version = 1,
        )
        provider.ensureFolderAndFiles(WakeHookJson.encode(listOf(remoteAlarm)), "# WakeHook")

        val outcome = engine.sync()

        val stored = repo.get("r1")
        assertThat(stored).isNotNull()
        assertThat(stored!!.enabled).isTrue()
        assertThat(stored.ackState).isEqualTo("scheduled")

        assertThat(pendingBroadcast("r1")).isNotNull()

        val remoteAfter = WakeHookJson.decode(provider.readJson()!!.content)
        val remoteR1 = remoteAfter.alarms.first { it.id == "r1" }
        assertThat(remoteR1.ackState).isEqualTo("scheduled")

        assertThat(notifier.added.map { it.id }).contains("r1")
        assertThat(outcome.added).isEqualTo(1)
    }

    @Test fun localOnlyAlarm_isWrittenToRemoteAfterSync() = runTest {
        val localAlarm = Alarm(id = "l1", label = "Gym", hour = 6, minute = 45, source = "local", version = 0)
        repo.upsert(localAlarm)

        engine.sync()

        val remote = WakeHookJson.decode(provider.readJson()!!.content)
        assertThat(remote.alarms.map { it.id }).contains("l1")
    }

    @Test fun remoteDisablesAlarm_localBecomesDisabledCancelledAndAcked() = runTest {
        val local = Alarm(id = "a1", label = "Wake", hour = 7, minute = 0, enabled = true, source = "local", version = 1)
        repo.upsert(local)
        scheduler.schedule(local)
        assertThat(pendingBroadcast("a1")).isNotNull()

        val remoteDisable = Alarm(
            id = "a1", label = "Wake", hour = 7, minute = 0,
            enabled = false, source = "claude", version = 2,
        )
        provider.ensureFolderAndFiles(WakeHookJson.encode(listOf(remoteDisable)), "# WakeHook")

        engine.sync()

        val stored = repo.get("a1")
        assertThat(stored).isNotNull()
        assertThat(stored!!.enabled).isFalse()
        assertThat(stored.ackState).isEqualTo("disabled")
        assertThat(pendingBroadcast("a1")).isNull()
    }

    @Test fun locallyTombstonedAlarm_stillPresentInRemote_staysDeleted_notRescheduled() = runTest {
        val local = Alarm(id = "a1", label = "Deleted", hour = 7, minute = 0, source = "local", version = 1)
        repo.upsert(local)
        scheduler.schedule(local)
        // Mirrors AlarmListViewModel.delete(): repo removal + scheduler cancel + tombstone,
        // all before the agent's copy has caught up.
        tombstones.add("a1")
        repo.delete("a1")
        scheduler.cancel("a1")

        val remoteStillHasIt = Alarm(
            id = "a1", label = "Still remote", hour = 8, minute = 0,
            enabled = true, source = "claude", version = 99,
        )
        provider.ensureFolderAndFiles(WakeHookJson.encode(listOf(remoteStillHasIt)), "# WakeHook")

        engine.sync()

        assertThat(repo.get("a1")).isNull()
        assertThat(pendingBroadcast("a1")).isNull()

        val remote = WakeHookJson.decode(provider.readJson()!!.content)
        assertThat(remote.alarms.map { it.id }).doesNotContain("a1")
    }
}
