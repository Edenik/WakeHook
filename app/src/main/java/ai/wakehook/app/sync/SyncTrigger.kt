package ai.wakehook.app.sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Enqueues [SyncWorker] runs: a 15-minute floor via periodic WorkManager, plus event-driven
 * one-shots (app open, mutation, alarm fire, manual "Sync now") that all collapse into a single
 * pending/running unit of work via [ExistingWorkPolicy.REPLACE].
 */
object SyncTrigger {
    const val PERIODIC_WORK_NAME = "wakehook-sync-periodic"
    const val NOW_WORK_NAME = "wakehook-sync-now"

    fun schedulePeriodic(context: Context) {
        // Called from WakeHookApp.onCreate(); WorkManager's own ContentProvider normally
        // finishes initializing before Application.onCreate() runs on a real device, but some
        // test harnesses (Robolectric) don't guarantee that ordering. Treat "not initialized
        // yet" as a harmless no-op rather than crashing app startup / unrelated tests.
        runCatching {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }

    fun now(context: Context) {
        runCatching {
            // Deliberately NOT expedited: WorkManager's expedited path requires the worker to
            // implement getForegroundInfo() on API 26-30 (it falls back to a foreground service
            // there), which SyncWorker doesn't -- that combination crashes at run time. A plain
            // one-time request runs within a few seconds via WorkManager's normal scheduling,
            // which is acceptable for event/manual syncs and avoids the crash entirely.
            val request = OneTimeWorkRequestBuilder<SyncWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                NOW_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
