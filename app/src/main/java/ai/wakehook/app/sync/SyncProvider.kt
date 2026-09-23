package ai.wakehook.app.sync

/** A remote copy of `wakehook.json`: its content plus an opaque version marker for change detection. */
data class RemoteFile(val content: String, val etag: String)

/**
 * Abstracts the storage backend (Google Drive in production) behind the sync engine, so the
 * engine can be tested end-to-end with an in-memory fake.
 */
interface SyncProvider {
    /** Creates the `WakeHook/` folder + `wakehook.json`/`wakehook.md` if they don't already exist. Never overwrites. */
    suspend fun ensureFolderAndFiles(seedJson: String, seedMd: String)

    /** Reads `wakehook.json`, or null if it doesn't exist yet. */
    suspend fun readJson(): RemoteFile?

    /** Overwrites `wakehook.json` with [content], returning its new etag. */
    suspend fun writeJson(content: String): String

    /** Whether the provider currently has a usable connection (e.g. signed in + network up). */
    suspend fun isConnected(): Boolean
}
