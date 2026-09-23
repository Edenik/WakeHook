package ai.wakehook.app.sync

import android.content.Context

/**
 * Tracks ids of alarms deleted locally, so a delete that hasn't yet propagated to the remote
 * `wakehook.json` doesn't get resurrected by the next sync. The #1 repository does not track
 * tombstones itself; this is the source of truth for them.
 */
interface TombstoneStore {
    suspend fun local(): Set<String>
    suspend fun add(id: String)
}

/** [TombstoneStore] backed by a `StringSet` in [Context.getSharedPreferences]. */
class PrefsTombstoneStore(private val context: Context) : TombstoneStore {
    private val prefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun local(): Set<String> =
        prefs.getStringSet(KEY_TOMBSTONES, emptySet()) ?: emptySet()

    override suspend fun add(id: String) {
        val current = local()
        prefs.edit().putStringSet(KEY_TOMBSTONES, current + id).apply()
    }

    companion object {
        private const val PREFS_NAME = "wakehook_sync"
        private const val KEY_TOMBSTONES = "tombstones"
    }
}
