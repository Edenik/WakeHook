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
import java.io.IOException
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

    @Test fun firstConnect_onFreshProvider_seedsExampleAlarmsAndAgentGuide() = runTest {
        assertThat(provider.readJson()).isNull()

        engine.firstConnect()

        val remote = WakeHookJson.decode(provider.readJson()!!.content)
        val seedIds = Seed.exampleAlarms().map { it.id }
        assertThat(remote.alarms.map { it.id }).containsExactlyElementsIn(seedIds)
        assertThat(remote.alarms.all { !it.enabled }).isTrue()

        val stored = seedIds.map { repo.get(it) }
        assertThat(stored).doesNotContain(null)
        assertThat(stored.all { it!!.enabled == false }).isTrue()

        assertThat(provider.md).isEqualTo(Seed.MARKDOWN)
    }

    /**
     * FIX 2 (IMPORTANT) regression test: a read failure (network/auth/etc, simulated here via
     * `failRead`) used to be swallowed by GoogleDriveProvider.readJson() and returned as `null`
     * -- indistinguishable from "the remote file genuinely doesn't exist yet". SyncEngine would
     * then treat that as an empty remote and write the (empty-of-remote-content) merged set back,
     * destroying whatever an agent had written. This asserts the failure instead aborts the pass
     * before anything is written, leaving the remote content untouched.
     */
    @Test fun readFailure_abortsSyncBeforeAnyWrite_remoteContentUntouched() = runTest {
        val remoteAgentAlarm = Alarm(
            id = "agent1", label = "Standup", hour = 9, minute = 0,
            enabled = true, source = "claude", version = 1,
        )
        provider.ensureFolderAndFiles(WakeHookJson.encode(listOf(remoteAgentAlarm)), "# WakeHook")
        provider.failRead = true

        var threw = false
        try {
            engine.sync()
        } catch (e: IOException) {
            threw = true
        }
        assertThat(threw).isTrue()

        provider.failRead = false
        val remote = WakeHookJson.decode(provider.readJson()!!.content)
        assertThat(remote.alarms.map { it.id }).contains("agent1")
        // Local repo/state must also be untouched -- the read failure happens before anything
        // else in the pass runs.
        assertThat(repo.getAll()).isEmpty()
    }

    /**
     * FIX 3 (IMPORTANT) regression test: `SyncEngine` now passes the etag captured from its
     * `readJson()` into `writeJson()` as a compare-and-swap. If something else wrote
     * `wakehook.json` in between (simulated here via `FakeSyncProvider.onBeforeWrite`), the write
     * must throw a conflict instead of clobbering that other write, and the engine surfaces that
     * failure (as a retryable IOException) rather than swallowing it.
     */
    @Test fun writeConflict_duringSync_abortsWithoutClobberingRemote() = runTest {
        val remoteAlarm = Alarm(
            id = "c1", label = "Standup", hour = 9, minute = 0,
            enabled = true, source = "claude", version = 1,
        )
        provider.ensureFolderAndFiles(WakeHookJson.encode(listOf(remoteAlarm)), "# WakeHook")
        // A local-only alarm guarantees the merge needs to write remote at all.
        repo.upsert(Alarm(id = "l1", label = "Gym", hour = 6, minute = 45, source = "local", version = 0))

        provider.onBeforeWrite = {
            // Simulate a concurrent external writer landing between the engine's read and write.
            provider.simulateExternalWrite(
                WakeHookJson.encode(listOf(remoteAlarm.copy(label = "Changed elsewhere")))
            )
        }

        var threw = false
        try {
            engine.sync()
        } catch (e: ConflictException) {
            threw = true
        }
        assertThat(threw).isTrue()

        val after = WakeHookJson.decode(provider.readJson()!!.content)
        assertThat(after.alarms.first { it.id == "c1" }.label).isEqualTo("Changed elsewhere")
    }

    /**
     * FIX 4 (IMPORTANT) regression test: SyncEngine must not blindly `repo.upsert()` a merge
     * winner computed from a stale `repo.getAll()` snapshot -- if a concurrent local edit landed
     * with a HIGHER version than that merge winner by the time the engine gets to reschedule/
     * upsert it, the concurrent edit must win and NOT be overwritten. A [RaceStaleSnapshotRepo]
     * double simulates the race: `getAll()` returns the stale (pre-race) snapshot used for
     * merging, while the real underlying repo is updated with a newer version in between.
     */
    @Test fun concurrentNewerLocalEdit_duringSync_isNotOverwrittenByStaleMergedValue() = runTest {
        val original = Alarm(id = "s1", label = "Original", hour = 7, minute = 0, source = "local", version = 1)
        repo.upsert(original)
        val staleSnapshot = repo.getAll()

        val remoteStale = Alarm(
            id = "s1", label = "Remote (stale by the time engine acts)", hour = 7, minute = 30,
            enabled = true, source = "claude", version = 2,
        )
        provider.ensureFolderAndFiles(WakeHookJson.encode(listOf(remoteStale)), "# WakeHook")

        // The race: a concurrent local edit lands with a HIGHER version than the merge winner
        // (remoteStale, v2) will end up with, before the engine reschedules/upserts it.
        val racedLocalEdit = original.copy(label = "Edited concurrently", version = 3)
        repo.upsert(racedLocalEdit)

        val racedRepo = RaceStaleSnapshotRepo(repo, staleSnapshot)
        val racedEngine = SyncEngine(provider, racedRepo, scheduler, tombstones, notifier, state)

        racedEngine.sync()

        val stored = repo.get("s1")
        assertThat(stored).isEqualTo(racedLocalEdit)
    }

    /** [AlarmRepository] double whose [getAll] returns a fixed pre-captured snapshot (simulating
     * a sync pass that already read `local` before a concurrent edit landed), while every other
     * call (notably [get]) delegates live to [delegate] -- letting a test observe the "the repo
     * has moved on since the merge snapshot was taken" race SyncEngine must guard against. */
    private class RaceStaleSnapshotRepo(
        private val delegate: AlarmRepository,
        private val staleSnapshot: List<Alarm>,
    ) : AlarmRepository by delegate {
        override suspend fun getAll(): List<Alarm> = staleSnapshot
    }
}
