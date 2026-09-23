package ai.wakehook.app.sync

import android.content.Context

/** Persists last-sync time + connected flag in [Context.getSharedPreferences]. */
class SyncState(private val context: Context) {
    private val prefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun lastSync(): Long = prefs.getLong(KEY_LAST_SYNC, 0L)

    fun setLastSync(atMillis: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, atMillis).apply()
    }

    fun connected(): Boolean = prefs.getBoolean(KEY_CONNECTED, false)

    fun setConnected(connected: Boolean) {
        prefs.edit().putBoolean(KEY_CONNECTED, connected).apply()
    }

    companion object {
        internal const val PREFS_NAME = "wakehook_sync"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_CONNECTED = "connected"
    }
}
