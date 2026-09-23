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

    /** Active Drive provider, restored by [ai.wakehook.app.WakeHookApp] before workers can run. */
    @Volatile var syncProvider: SyncProvider? = null
}

/** Reaches the app-wide [AppContainer] from any [Context], e.g. inside a [androidx.work.CoroutineWorker]. */
fun Context.appContainer(): AppContainer = (applicationContext as WakeHookApp).container
