package ai.wakehook.app.ui

import android.content.Context
import ai.wakehook.app.WakeHookApp
import ai.wakehook.app.alarm.AlarmScheduler
import ai.wakehook.app.data.AlarmDatabase
import ai.wakehook.app.data.AlarmRepository
import ai.wakehook.app.data.RoomAlarmRepository
import ai.wakehook.app.sync.PrefsTombstoneStore
import ai.wakehook.app.sync.SyncProvider
import ai.wakehook.app.sync.TombstoneStore

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val repository: AlarmRepository = RoomAlarmRepository(AlarmDatabase.get(context).alarmDao())
    val scheduler: AlarmScheduler = AlarmScheduler(context)

    /** Records ids deleted locally, so [ai.wakehook.app.sync.SyncEngine] doesn't resurrect them. */
    val tombstones: TombstoneStore = PrefsTombstoneStore(appContext)

    /**
     * The active [SyncProvider], set once the user signs in to Google Drive. The real provider
     * is wired here in a later task; until then this stays null and [ai.wakehook.app.sync.SyncWorker]
     * treats that as "not connected" and no-ops.
     */
    @Volatile var syncProvider: SyncProvider? = null
}

/** Reaches the app-wide [AppContainer] from any [Context], e.g. inside a [androidx.work.CoroutineWorker]. */
fun Context.appContainer(): AppContainer = (applicationContext as WakeHookApp).container
