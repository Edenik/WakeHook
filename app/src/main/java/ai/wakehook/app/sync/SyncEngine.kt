package ai.wakehook.app.sync

import ai.wakehook.app.alarm.AlarmScheduler
import ai.wakehook.app.data.AlarmRepository
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
    suspend fun sync(): SyncOutcome {
        val remoteFile = provider.readJson()
        val rawRemoteAlarms = remoteFile?.let { WakeHookJson.decode(it.content).alarms } ?: emptyList()

        val local = repo.getAll()
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

        var ackChanged = false
        val ackedAlarms = merge.merged.map { alarm ->
            val acked = if (alarm.enabled) {
                try {
                    scheduler.cancel(alarm.id)
                    scheduler.schedule(alarm)
                    alarm.copy(ackState = "scheduled", ackAt = nowIso(), ackError = "")
                } catch (e: Exception) {
                    alarm.copy(ackState = "error", ackAt = nowIso(), ackError = e.message ?: e.toString())
                }
            } else {
                scheduler.cancel(alarm.id)
                alarm.copy(ackState = "disabled", ackAt = nowIso(), ackError = "")
            }
            if (acked.ackState != alarm.ackState) ackChanged = true
            acked
        }

        for (alarm in ackedAlarms) {
            repo.upsert(alarm)
        }

        val wroteRemote = merge.toWriteRemote || ackChanged
        if (wroteRemote) {
            provider.writeJson(WakeHookJson.encode(ackedAlarms))
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
