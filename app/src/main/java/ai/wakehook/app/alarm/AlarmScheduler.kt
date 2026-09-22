package ai.wakehook.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import ai.wakehook.app.MainActivity
import ai.wakehook.app.data.AlarmRepository
import ai.wakehook.app.domain.Alarm
import ai.wakehook.app.domain.AlarmTime
import java.time.ZonedDateTime

object AlarmIntents {
    const val EXTRA_ID = "alarmId"
    const val EXTRA_LABEL = "label"
    const val EXTRA_SNOOZE = "snooze"
    /** One stable request code per alarm, reused for its snooze. */
    fun requestCode(alarmId: String): Int = alarmId.hashCode()
}

class AlarmScheduler(private val context: Context) {
    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()

    fun schedule(alarm: Alarm) {
        val at = AlarmTime.nextTrigger(alarm, ZonedDateTime.now()) ?: return
        am.setAlarmClock(AlarmManager.AlarmClockInfo(at, showIntent()), fireIntent(alarm.id, alarm.label))
    }

    fun scheduleSnooze(alarmId: String, label: String, atMillis: Long) {
        am.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, showIntent()),
            fireIntent(alarmId, label, snooze = true))
    }

    fun cancel(alarmId: String) {
        val pi = fireIntent(alarmId, null)
        am.cancel(pi)
        pi.cancel()
    }

    suspend fun rescheduleAll(repo: AlarmRepository) {
        for (a in repo.getAll()) {
            cancel(a.id)
            if (a.enabled) schedule(a)
        }
    }

    private fun fireIntent(alarmId: String, label: String?, snooze: Boolean = false): PendingIntent {
        val i = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmIntents.EXTRA_ID, alarmId)
            putExtra(AlarmIntents.EXTRA_LABEL, label ?: "")
            putExtra(AlarmIntents.EXTRA_SNOOZE, snooze)
        }
        return PendingIntent.getBroadcast(context, AlarmIntents.requestCode(alarmId), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun showIntent(): PendingIntent {
        val i = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, 0, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
