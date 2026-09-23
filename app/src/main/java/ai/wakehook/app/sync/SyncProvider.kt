package ai.wakehook.app.sync

import java.io.IOException

/** A remote copy of `wakehook.json`: its content plus an opaque version marker for change detection. */
data class RemoteFile(val content: String, val etag: String)

/**
 * Thrown by [SyncProvider.writeJson] when a non-null `expectedEtag` no longer matches the
 * remote file's current etag -- i.e. something else wrote `wakehook.json` since it was last
 * read. Extends [IOException] so it's treated the same as any other transient sync failure by
 * callers that already retry on [IOException] (see `SyncWorker`).
 */
class ConflictException(message: String = "wakehook.json changed remotely since last read (etag conflict)") :
    IOException(message)

/**
 * Abstracts the storage backend (Google Drive in production) behind the sync engine, so the
 * engine can be tested end-to-end with an in-memory fake.
 */
interface SyncProvider {
    /** Creates the `WakeHook/` folder + `wakehook.json`/`wakehook.md` if they don't already exist. Never overwrites. */
    suspend fun ensureFolderAndFiles(seedJson: String, seedMd: String)

    /**
     * Reads `wakehook.json`, or null if it genuinely doesn't exist yet (first-connect/seed
     * path). Any OTHER read failure (network, auth, etc) must be thrown, not swallowed into a
     * null -- callers must not mistake "read failed" for "remote is empty".
     */
    suspend fun readJson(): RemoteFile?

    /**
     * Overwrites `wakehook.json` with [content], returning its new etag. If [expectedEtag] is
     * non-null, this is a compare-and-swap: if the remote file's current etag no longer matches
     * [expectedEtag], throws [ConflictException] and does NOT write. Pass null to skip the
     * check (e.g. the very first write, when there's nothing to compare against).
     */
    suspend fun writeJson(content: String, expectedEtag: String?): String

    /** Whether the provider currently has a usable connection (e.g. signed in + network up). */
    suspend fun isConnected(): Boolean
}
