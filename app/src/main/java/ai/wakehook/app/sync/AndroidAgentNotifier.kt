package ai.wakehook.app.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ai.wakehook.app.MainActivity
import ai.wakehook.app.R
import ai.wakehook.app.domain.Alarm

/**
 * Real Android [AgentNotifier]: posts a low-priority notification per agent-added/changed alarm
 * on the `agent_activity` channel, distinct from the high-priority ring channel (`alarms`).
 */
class AndroidAgentNotifier(private val context: Context) : AgentNotifier {
    companion object { const val CHANNEL = "agent_activity" }

    override fun notifyAgentActivity(added: List<Alarm>, changed: List<Alarm>) {
        if (added.isEmpty() && changed.isEmpty()) return

        ensureChannel()
        val nmCompat = NotificationManagerCompat.from(context)
        for (alarm in added) post(nmCompat, alarm, changed = false)
        for (alarm in changed) post(nmCompat, alarm, changed = true)
    }

    private fun post(nmCompat: NotificationManagerCompat, alarm: Alarm, changed: Boolean) {
        val title = if (changed) "${alarm.source} changed an alarm" else "${alarm.source} set an alarm"
        val time = "%02d:%02d".format(alarm.hour, alarm.minute)
        val text = if (alarm.label.isBlank()) time else "$time · ${alarm.label}"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val requestCode = requestCode(alarm.id)
        val pi = PendingIntent.getActivity(context, requestCode, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        nmCompat.notify(requestCode, n)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(CHANNEL, "Agent activity", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }
    }

    /** Stable notification id per alarm, distinct from the ring notification's id namespace. */
    private fun requestCode(alarmId: String): Int = ("agent_$alarmId").hashCode()
}
