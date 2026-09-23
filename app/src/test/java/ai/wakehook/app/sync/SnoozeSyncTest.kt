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

/**
 * Covers Task 9: a snooze (persisted with `snoozedUntil` + a version bump, mirroring
 * `RingActivity.persistSnooze`) survives a sync and reaches the remote `wakehook.json`; a local
 * delete tombstone (mirroring `AlarmListViewModel.delete`) keeps the id out of the remote set
 * after sync, same as Task 5's tombstone-delete behavior.
 */
@RunWith(RobolectricTestRunner::class)
class SnoozeSyncTest {
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

    private fun pendingSnoozeBroadcast(id: String): PendingIntent? = PendingIntent.getBroadcast(
        app, AlarmIntents.snoozeRequestCode(id),
        Intent(app, AlarmReceiver::class.java),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )

    /**
     * FIX 1 (CRITICAL) regression test: a sync pass used to unconditionally cancel + reschedule
     * the NORMAL occurrence for every enabled merged alarm, ignoring `snoozedUntil` entirely.
     * `AlarmScheduler.cancel()` cancels BOTH the normal and snooze PendingIntents, so a sync
     * racing a live snooze (e.g. triggered by RingActivity.persistSnooze's own SyncTrigger.now())
     * would kill the armed snooze PendingIntent and never re-arm it -- the snoozed alarm would
     * simply never ring again for a connected user. This asserts the snooze PendingIntent is
     * still armed (not left unscheduled) after sync().
     */
    @Test fun liveSnooze_survivesSync_snoozePendingIntentStaysArmed_normalOneDoesNot() = runTest {
        val snoozedUntil = System.currentTimeMillis() + 5 * 60 * 1000L
        val original = Alarm(id = "a3", label = "Wake", hour = 7, minute = 0, source = "local", version = 1)
        repo.upsert(original)
        val snoozed = original.copy(snoozedUntil = snoozedUntil, version = original.version + 1)
        repo.upsert(snoozed)
        // Mirrors RingActivity.onSnooze(): the snooze PendingIntent is armed at snooze time,
        // before any sync runs.
        scheduler.scheduleSnooze(snoozed.id, snoozed.label, snoozedUntil)

        engine.sync()

        assertThat(pendingSnoozeBroadcast("a3")).isNotNull()
        assertThat(pendingBroadcast("a3")).isNull()

        val stored = repo.get("a3")
        assertThat(stored).isNotNull()
        assertThat(stored!!.snoozedUntil).isEqualTo(snoozedUntil)
        assertThat(stored.ackState).isEqualTo("scheduled")
    }

    /** FIX 1: an expired `snoozedUntil` (in the past) must be cleared and the normal occurrence
     * scheduled instead -- not left dangling forever. */
    @Test fun expiredSnooze_isClearedAndNormalOccurrenceIsScheduled() = runTest {
        val pastSnooze = System.currentTimeMillis() - 60_000L
        val original = Alarm(id = "a4", label = "Wake", hour = 7, minute = 0, source = "local", version = 1)
        repo.upsert(original)
        val snoozed = original.copy(snoozedUntil = pastSnooze, version = original.version + 1)
        repo.upsert(snoozed)

        engine.sync()

        assertThat(pendingBroadcast("a4")).isNotNull()
        assertThat(pendingSnoozeBroadcast("a4")).isNull()

        val stored = repo.get("a4")
        assertThat(stored).isNotNull()
        assertThat(stored!!.snoozedUntil).isNull()
        assertThat(stored.ackState).isEqualTo("scheduled")
    }

    @Test fun snoozedAlarm_survivesSync_andCarriesSnoozedUntilInRemote() = runTest {
        val snoozedUntil = System.currentTimeMillis() + 10 * 60 * 1000L
        val local = Alarm(id = "a1", label = "Wake", hour = 7, minute = 0, source = "local", version = 1)
        repo.upsert(local)

        // Mirrors RingActivity.persistSnooze(): set snoozedUntil + bump version on the alarm.
        val snoozed = local.copy(snoozedUntil = snoozedUntil, version = local.version + 1)
        repo.upsert(snoozed)

        engine.sync()

        val stored = repo.get("a1")
        assertThat(stored).isNotNull()
        assertThat(stored!!.snoozedUntil).isEqualTo(snoozedUntil)

        val remote = WakeHookJson.decode(provider.readJson()!!.content)
        val remoteA1 = remote.alarms.first { it.id == "a1" }
        assertThat(remoteA1.snoozedUntil).isEqualTo(snoozedUntil)
    }

    @Test fun locallyDeletedAlarm_tombstoned_absentFromRemoteAfterSync() = runTest {
        val local = Alarm(id = "a2", label = "Deleted", hour = 7, minute = 0, source = "local", version = 1)
        repo.upsert(local)
        scheduler.schedule(local)
        provider.ensureFolderAndFiles(WakeHookJson.encode(listOf(local)), "# WakeHook")

        // Mirrors AlarmListViewModel.delete(): repo removal + scheduler cancel + tombstone.
        repo.delete("a2")
        scheduler.cancel("a2")
        tombstones.add("a2")

        engine.sync()

        assertThat(repo.get("a2")).isNull()

        val remote = WakeHookJson.decode(provider.readJson()!!.content)
        assertThat(remote.alarms.map { it.id }).doesNotContain("a2")
    }
}
