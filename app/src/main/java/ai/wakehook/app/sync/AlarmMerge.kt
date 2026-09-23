package ai.wakehook.app.sync

import ai.wakehook.app.domain.Alarm

data class MergeResult(
    val merged: List<Alarm>,          // authoritative set to persist locally
    val toWriteRemote: Boolean,       // whether local changed the remote set
    val added: List<Alarm>,           // alarms new-from-remote (source != "local") -> notify
    val changed: List<Alarm>,         // alarms changed-from-remote by an agent -> notify
)

object AlarmMerge {

    fun merge(
        local: List<Alarm>,
        remote: List<Alarm>,
        localTombstones: Set<String>,
        remoteTombstones: Set<String>,
    ): MergeResult {
        val tombstones = localTombstones + remoteTombstones
        val localById = local.associateBy { it.id }
        val remoteById = remote.associateBy { it.id }
        val ids = (localById.keys + remoteById.keys) - tombstones

        val merged = mutableListOf<Alarm>()
        val added = mutableListOf<Alarm>()
        val changed = mutableListOf<Alarm>()

        for (id in ids) {
            val localAlarm = localById[id]
            val remoteAlarm = remoteById[id]

            val winner: Alarm
            val remoteWon: Boolean
            when {
                localAlarm == null -> {
                    winner = remoteAlarm!!
                    remoteWon = true
                }
                remoteAlarm == null -> {
                    winner = localAlarm
                    remoteWon = false
                }
                remoteAlarm.version > localAlarm.version -> {
                    winner = remoteAlarm
                    remoteWon = true
                }
                else -> {
                    // tie or local higher -> local wins
                    winner = localAlarm
                    remoteWon = false
                }
            }

            merged.add(winner)

            if (remoteWon && winner.source != "local") {
                if (localAlarm == null) {
                    added.add(winner)
                } else {
                    changed.add(winner)
                }
            }
        }

        // Compare against remote as-is (unfiltered): if a tombstone dropped an id that
        // remote still has, that's a real mismatch we need to write back.
        val mergedKeySet = merged.map { contentKey(it) }.toSet()
        val remoteKeySet = remote.map { contentKey(it) }.toSet()
        val toWriteRemote = mergedKeySet != remoteKeySet

        return MergeResult(
            merged = merged,
            toWriteRemote = toWriteRemote,
            added = added,
            changed = changed,
        )
    }

    private fun contentKey(alarm: Alarm): String = "${alarm.id}:${alarm.version}:$alarm"
}
