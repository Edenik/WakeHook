package ai.wakehook.app.sync

/** In-memory [TombstoneStore] test double. */
class FakeTombstoneStore : TombstoneStore {
    private val ids = mutableSetOf<String>()

    override suspend fun local(): Set<String> = ids.toSet()

    override suspend fun add(id: String) {
        ids.add(id)
    }

    override suspend fun remove(id: String) {
        ids.remove(id)
    }
}
