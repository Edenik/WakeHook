package ai.wakehook.app.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat

data class PermissionStatus(
    val exactAlarm: Boolean,
    val notifications: Boolean,
    val batteryExempt: Boolean,
    val fullScreenIntent: Boolean,
)

object PermissionState {
    fun read(context: Context): PermissionStatus {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()

        val notif = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val battery = pm.isIgnoringBatteryOptimizations(context.packageName)

        // Android 14+ (API 34) can silently revoke a notification's ability to launch a
        // full-screen intent, which is the only thing that auto-launches RingActivity. Below
        // API 34 there's no such gate, so treat it as granted.
        val fullScreenIntent = Build.VERSION.SDK_INT < 34 ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

        return PermissionStatus(exact, notif, battery, fullScreenIntent)
    }
}
