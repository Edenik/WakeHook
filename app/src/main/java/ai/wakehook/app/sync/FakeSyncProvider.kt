package ai.wakehook.app.sync

import java.io.IOException

/** In-memory [SyncProvider] double used to test the sync engine without real Drive access. */
class FakeSyncProvider : SyncProvider {
    var content: String? = null
        private set
    var md: String? = null
        private set
    var connected: Boolean = true

    /**
     * Test hook: when true, [readJson] throws [IOException] instead of returning content/null --
     * simulates a genuine read failure (network/auth/etc), distinct from "file doesn't exist
     * yet" (which is a `null` return, not an exception).
     */
    var failRead: Boolean = false

    /**
     * Test hook: invoked synchronously at the very start of [writeJson], before the CAS check --
     * lets a test simulate an external writer (e.g. another device) racing in between a caller's
     * read and its write.
     */
    var onBeforeWrite: (() -> Unit)? = null

    private var etagCounter: Int = 0
    private val etag: String get() = etagCounter.toString()

    override suspend fun ensureFolderAndFiles(seedJson: String, seedMd: String) {
        if (content == null) {
            content = seedJson
            md = seedMd
            etagCounter++
        }
    }

    override suspend fun readJson(): RemoteFile? {
        if (failRead) throw IOException("FakeSyncProvider: simulated read failure")
        val current = content ?: return null
        return RemoteFile(current, etag)
    }

    override suspend fun writeJson(content: String, expectedEtag: String?): String {
        onBeforeWrite?.invoke()
        if (expectedEtag != null && expectedEtag != etag) {
            throw ConflictException()
        }
        this.content = content
        etagCounter++
        return etag
    }

    /**
     * Test-only: simulates an external write (e.g. another device) landing outside this
     * provider's normal CAS-checked [writeJson] path, bumping the etag out from under a caller
     * that already read the old one.
     */
    fun simulateExternalWrite(newContent: String) {
        content = newContent
        etagCounter++
    }

    override suspend fun isConnected(): Boolean = connected
}
