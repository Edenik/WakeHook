package ai.wakehook.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import ai.wakehook.app.R
import ai.wakehook.app.data.AlarmDatabase
import ai.wakehook.app.data.RoomAlarmRepository
import ai.wakehook.app.sync.SyncTrigger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class AlarmReceiver : BroadcastReceiver() {
    companion object { const val CHANNEL = "alarms" }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(AlarmIntents.EXTRA_ID) ?: return
        var label = intent.getStringExtra(AlarmIntents.EXTRA_LABEL) ?: ""
        val snooze = intent.getBooleanExtra(AlarmIntents.EXTRA_SNOOZE, false)
        // goAsync() extends the broadcast's lifetime on a real device but returns null
        // when onReceive is invoked directly (e.g. in Robolectric unit tests) rather than
        // dispatched through the system. Guard it and, more importantly, do the DB read +
        // rescheduling synchronously (blocking briefly on Dispatchers.IO) so onReceive does
        // not return until that work is actually done — deterministic in both production and
        // tests, instead of racing an unawaited coroutine.
        val pending = try { goAsync() } catch (_: IllegalStateException) { null }

        runBlocking(Dispatchers.IO) {
            try {
                if (!snooze) {
                    val repo = RoomAlarmRepository(AlarmDatabase.get(context).alarmDao())
                    val a = repo.get(id)
                    if (a != null) {
                        label = a.label
                        if (a.enabled && a.isRecurring) {
                            AlarmScheduler(context).schedule(a)
                        } else if (a.enabled && !a.isRecurring && !a.isDateBased) {
                            // One-time alarm: turn it off so BootReceiver.rescheduleAll (which
                            // relies on AlarmTime.nextTrigger, always future for one-time alarms)
                            // doesn't re-arm it for the next day, and the list shows it as off.
                            repo.upsert(a.copy(enabled = false))
                        }
                    }
                }
                showRing(context, id, label)
                SyncTrigger.now(context)
            } finally { pending?.finish() }
        }
    }

    private fun showRing(context: Context, id: String, label: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, context.getString(ai.wakehook.app.R.string.channel_alarms), NotificationManager.IMPORTANCE_HIGH)
            ch.setBypassDnd(true)
            nm.createNotificationChannel(ch)
        }
        val ring = Intent(context, RingActivity::class.java).apply {
            putExtra(AlarmIntents.EXTRA_ID, id)
            putExtra(AlarmIntents.EXTRA_LABEL, label)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullPi = PendingIntent.getActivity(context, AlarmIntents.requestCode(id), ring,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(label)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(fullPi, true)
            .build()
        nm.notify(AlarmIntents.requestCode(id), n)
    }
}
