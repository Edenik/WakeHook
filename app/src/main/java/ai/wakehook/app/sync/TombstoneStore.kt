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

    /** Stops tracking [id] as tombstoned -- called once its deletion has propagated to remote. */
    suspend fun remove(id: String)
}

/**
 * [TombstoneStore] backed by a `StringSet` in [Context.getSharedPreferences].
 *
 * [add]/[remove] synchronize on [lock] so a read-modify-write pair (read the current set, then
 * write current+/-id) can't race with another call and silently lose an update.
 */
class PrefsTombstoneStore(private val context: Context) : TombstoneStore {
    private val prefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val lock = Any()

    override suspend fun local(): Set<String> =
        prefs.getStringSet(KEY_TOMBSTONES, emptySet()) ?: emptySet()

    override suspend fun add(id: String) {
        synchronized(lock) {
            val current = prefs.getStringSet(KEY_TOMBSTONES, emptySet()) ?: emptySet()
            prefs.edit().putStringSet(KEY_TOMBSTONES, current + id).apply()
        }
    }

    override suspend fun remove(id: String) {
        synchronized(lock) {
            val current = prefs.getStringSet(KEY_TOMBSTONES, emptySet()) ?: emptySet()
            prefs.edit().putStringSet(KEY_TOMBSTONES, current - id).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "wakehook_sync"
        private const val KEY_TOMBSTONES = "tombstones"
    }
}
