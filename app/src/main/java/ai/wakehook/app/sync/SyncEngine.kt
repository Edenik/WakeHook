package ai.wakehook.app.sync

import ai.wakehook.app.alarm.AlarmScheduler
import ai.wakehook.app.data.AlarmRepository
import ai.wakehook.app.domain.Alarm
import java.time.Instant

/** Outcome of a single [SyncEngine.sync] pass. */
data class SyncOutcome(
    val added: Int,
    val changed: Int,
    val wroteRemote: Boolean,
)

/**
 * Orchestrates a single two-way sync pass: pull the remote `wakehook.json`, merge it with the
 * local repository (honoring tombstones + versions), (re)schedule the resulting alarms, ack the
 * result back to remote when anything changed, and notify the user of agent activity.
 */
class SyncEngine(
    val provider: SyncProvider,
    val repo: AlarmRepository,
    val scheduler: AlarmScheduler,
    val tombstones: TombstoneStore,
    val notifier: AgentNotifier,
    val state: SyncState,
) {
    /**
     * Seeds a brand-new `WakeHook/` folder with the example alarms + agent guide if the remote
     * `wakehook.json` doesn't exist yet, then runs a normal [sync] to pull it in locally.
     */
    suspend fun firstConnect() {
        if (provider.readJson() == null) {
            provider.ensureFolderAndFiles(WakeHookJson.encode(Seed.exampleAlarms()), Seed.MARKDOWN)
        }
        sync()
    }

    suspend fun sync(): SyncOutcome {
        // A thrown exception here means something OTHER than "file genuinely doesn't exist"
        // went wrong (network/auth/etc -- see GoogleDriveProvider.readJson / FakeSyncProvider's
        // `failRead`). Nothing has been mutated locally or remotely yet at this point, so let it
        // propagate straight out of `sync()`: the caller (SyncWorker) already treats IOException
        // as retryable, so this pass becomes a clean no-op instead of silently treating a read
        // failure like an empty remote and clobbering it on the next write.
        val remoteFile = provider.readJson()
        val remoteEtag = remoteFile?.etag
        val rawRemoteAlarms = remoteFile?.let { WakeHookJson.decode(it.content).alarms } ?: emptyList()

        val local = repo.getAll()
        val localById = local.associateBy { it.id }
        val localTombstones = tombstones.local()
        val remoteTombstones = emptySet<String>() // JSON does not model a deletedIds array yet

        val merge = AlarmMerge.merge(local, rawRemoteAlarms, localTombstones, remoteTombstones)
        val mergedIds = merge.merged.map { it.id }.toSet()

        // Drop local alarms that lost the merge (deleted remotely, or locally tombstoned).
        for (localAlarm in local) {
            if (localAlarm.id !in mergedIds) {
                repo.delete(localAlarm.id)
                scheduler.cancel(localAlarm.id)
            }
        }

        val now = System.currentTimeMillis()
        var ackChanged = false
        val skippedIds = mutableSetOf<String>()
        val ackedAlarms = mutableListOf<Alarm>()

        for (alarm in merge.merged) {
            // Guard against clobbering a newer local edit that raced ahead of this sync pass:
            // if the repo's CURRENT value for this id is a strictly higher version than what
            // won the merge (computed from the `local` snapshot taken above), a concurrent local
            // edit landed while this pass was running. Leave it alone entirely -- no reschedule,
            // no upsert -- but keep its current value in the remote-write payload below so it
            // isn't dropped from `wakehook.json`.
            val existingLocal = repo.get(alarm.id)
            if (existingLocal != null && existingLocal.version > alarm.version) {
                skippedIds.add(alarm.id)
                ackedAlarms.add(existingLocal)
                continue
            }

            // A snooze that already expired by "now" is stale metadata -- clear it before
            // deciding what to (re)schedule so it doesn't linger forever.
            val normalized = if (alarm.snoozedUntil != null && alarm.snoozedUntil <= now) {
                alarm.copy(snoozedUntil = null)
            } else {
                alarm
            }

            val acked = if (!normalized.enabled) {
                scheduler.cancel(normalized.id)
                normalized.copy(ackState = "disabled", ackAt = nowIso(), ackError = "")
            } else {
                try {
                    scheduler.cancel(normalized.id)
                    if (normalized.snoozedUntil != null && normalized.snoozedUntil > now) {
                        // A live snooze must survive sync: re-arm the snooze fire only. Scheduling
                        // the normal occurrence here would cancel the very snooze PendingIntent
                        // RingActivity just armed, and the snoozed alarm would never re-ring.
                        scheduler.scheduleSnooze(normalized.id, normalized.label, normalized.snoozedUntil)
                    } else {
                        scheduler.schedule(normalized)
                    }
                    normalized.copy(ackState = "scheduled", ackAt = nowIso(), ackError = "")
                } catch (e: Exception) {
                    normalized.copy(ackState = "error", ackAt = nowIso(), ackError = e.message ?: e.toString())
                }
            }
            if (acked.ackState != alarm.ackState) ackChanged = true
            ackedAlarms.add(acked)
        }

        for (alarm in ackedAlarms) {
            if (alarm.id in skippedIds) continue
            if (localById[alarm.id] != alarm) {
                repo.upsert(alarm)
            }
        }

        val wroteRemote = merge.toWriteRemote || ackChanged
        if (wroteRemote) {
            // CAS against the etag captured from the read above: if remote changed underneath us
            // since then, this throws ConflictException (an IOException) instead of clobbering
            // it. Nothing further below runs; the next sync pass re-reads and reconciles.
            provider.writeJson(WakeHookJson.encode(ackedAlarms), remoteEtag)
        }

        // Reaching here means the write above (if any) landed, or none was needed -- either way
        // remote is now known to match `ackedAlarms` (if written) or its as-read state (if not).
        // Any local tombstone whose id isn't in that final remote set has propagated: stop
        // tracking it so the tombstone set doesn't grow forever.
        val finalRemoteIds = if (wroteRemote) ackedAlarms.map { it.id }.toSet() else rawRemoteAlarms.map { it.id }.toSet()
        for (id in localTombstones) {
            if (id !in finalRemoteIds) tombstones.remove(id)
        }

        notifier.notifyAgentActivity(merge.added, merge.changed)

        state.setLastSync(System.currentTimeMillis())

        return SyncOutcome(
            added = merge.added.size,
            changed = merge.changed.size,
            wroteRemote = wroteRemote,
        )
    }

    private fun nowIso(): String = Instant.now().toString()
}
