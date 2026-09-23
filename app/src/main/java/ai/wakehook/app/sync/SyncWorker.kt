package ai.wakehook.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ai.wakehook.app.ui.appContainer
import java.io.IOException

/**
 * Runs a single [SyncEngine.sync] pass in the background, scheduled by [SyncTrigger].
 *
 * No-ops (success) when the app has no connected [SyncProvider] yet — the real Google Drive
 * provider is wired into [ai.wakehook.app.ui.AppContainer.syncProvider] on sign-in in a later
 * task; until then this worker has nothing to sync.
 */
class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer()
        val provider = container.syncProvider
        if (provider == null || !provider.isConnected()) return Result.success()

        val engine = SyncEngine(
            provider = provider,
            repo = container.repository,
            scheduler = container.scheduler,
            tombstones = PrefsTombstoneStore(applicationContext),
            notifier = AndroidAgentNotifier(applicationContext),
            state = SyncState(applicationContext),
        )
        return try {
            engine.sync()
            Result.success()
        } catch (e: IOException) {
            Result.retry()
        } catch (e: Exception) {
            Result.success()
        }
    }
}
