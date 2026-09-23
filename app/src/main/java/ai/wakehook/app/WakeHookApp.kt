package ai.wakehook.app

import android.app.Application
import ai.wakehook.app.sync.SyncTrigger
import ai.wakehook.app.ui.AppContainer

class WakeHookApp : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SyncTrigger.schedulePeriodic(this)
    }
}
