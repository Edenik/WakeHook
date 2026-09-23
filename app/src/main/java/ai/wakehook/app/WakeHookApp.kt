package ai.wakehook.app

import android.app.Application
import ai.wakehook.app.sync.GoogleDriveProvider
import ai.wakehook.app.sync.SyncState
import ai.wakehook.app.sync.SyncTrigger
import ai.wakehook.app.ui.AppContainer

class WakeHookApp : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        val driveProvider = GoogleDriveProvider.restore(this)
        container.syncProvider = driveProvider
        SyncState(this).setConnected(driveProvider != null)
        SyncTrigger.schedulePeriodic(this)
    }
}
