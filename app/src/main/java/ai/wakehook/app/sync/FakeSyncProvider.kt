package ai.wakehook.app.sync

/** In-memory [SyncProvider] double used to test the sync engine without real Drive access. */
class FakeSyncProvider : SyncProvider {
    var content: String? = null
        private set
    var md: String? = null
        private set
    var connected: Boolean = true

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
        val current = content ?: return null
        return RemoteFile(current, etag)
    }

    override suspend fun writeJson(content: String): String {
        this.content = content
        etagCounter++
        return etag
    }

    override suspend fun isConnected(): Boolean = connected
}
