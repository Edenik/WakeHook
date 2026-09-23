package ai.wakehook.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ai.wakehook.app.data.AlarmDatabase
import ai.wakehook.app.data.RoomAlarmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // Same fix as AlarmReceiver: goAsync() returns null under Robolectric (onReceive
        // invoked directly rather than dispatched through the system), so guard it and do
        // the reschedule synchronously on Dispatchers.IO to make onReceive deterministic in
        // both production and tests instead of racing an unawaited coroutine.
        val pending = try { goAsync() } catch (_: IllegalStateException) { null }

        runBlocking(Dispatchers.IO) {
            try {
                val repo = RoomAlarmRepository(AlarmDatabase.get(context).alarmDao())
                AlarmScheduler(context).rescheduleAll(repo)
            } finally { pending?.finish() }
        }
    }
}
