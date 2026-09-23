package ai.wakehook.app.ui.edit

import android.content.Context

/** Playback controls live on this device, alongside alarms, and are not shared with agents. */
data class AlarmPlaybackSettings(
    val soundEnabled: Boolean = true,
    val soundUri: String? = null,
    val vibrationEnabled: Boolean = true,
    val vibrationPattern: String = BASIC,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = NEW_ALARM_SNOOZE_MINUTES,
    /** 0 means unlimited snoozes. */
    val snoozeLimit: Int = NEW_ALARM_SNOOZE_LIMIT,
) {
    companion object {
        const val BASIC = "basic"
        const val GENTLE = "gentle"
        const val STRONG = "strong"
        const val NEW_ALARM_SNOOZE_MINUTES = 5
        const val NEW_ALARM_SNOOZE_LIMIT = 3
        const val EXISTING_ALARM_SNOOZE_MINUTES = 10
        const val SYSTEM_DEFAULT_SOUND = "default"
        const val SILENT_SOUND = "silent"
    }
}

class AlarmPlaybackSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(alarmId: String, isNewAlarm: Boolean = false): AlarmPlaybackSettings {
        val legacyMinutes = if (isNewAlarm) AlarmPlaybackSettings.NEW_ALARM_SNOOZE_MINUTES
        else AlarmPlaybackSettings.EXISTING_ALARM_SNOOZE_MINUTES
        val legacyLimit = if (isNewAlarm) AlarmPlaybackSettings.NEW_ALARM_SNOOZE_LIMIT else 0
        return AlarmPlaybackSettings(
            soundEnabled = prefs.getBoolean(key(alarmId, SOUND_ENABLED), true),
            soundUri = prefs.getString(key(alarmId, SOUND_URI), AlarmPlaybackSettings.SYSTEM_DEFAULT_SOUND)
                ?: AlarmPlaybackSettings.SILENT_SOUND,
            vibrationEnabled = prefs.getBoolean(key(alarmId, VIBRATION_ENABLED), true),
            vibrationPattern = prefs.getString(key(alarmId, VIBRATION_PATTERN), AlarmPlaybackSettings.BASIC)
                ?: AlarmPlaybackSettings.BASIC,
            snoozeEnabled = prefs.getBoolean(key(alarmId, SNOOZE_ENABLED), true),
            snoozeMinutes = prefs.getInt(key(alarmId, SNOOZE_MINUTES), legacyMinutes),
            snoozeLimit = prefs.getInt(key(alarmId, SNOOZE_LIMIT), legacyLimit),
        )
    }

    fun save(alarmId: String, value: AlarmPlaybackSettings) {
        prefs.edit()
            .putBoolean(key(alarmId, SOUND_ENABLED), value.soundEnabled)
            .putString(key(alarmId, SOUND_URI), value.soundUri ?: AlarmPlaybackSettings.SILENT_SOUND)
            .putBoolean(key(alarmId, VIBRATION_ENABLED), value.vibrationEnabled)
            .putString(key(alarmId, VIBRATION_PATTERN), value.vibrationPattern)
            .putBoolean(key(alarmId, SNOOZE_ENABLED), value.snoozeEnabled)
            .putInt(key(alarmId, SNOOZE_MINUTES), ALLOWED_SNOOZE_MINUTES.minBy { kotlin.math.abs(it - value.snoozeMinutes) })
            .putInt(key(alarmId, SNOOZE_LIMIT), ALLOWED_SNOOZE_LIMITS.minBy { kotlin.math.abs(it - value.snoozeLimit) })
            .apply()
    }

    fun snoozesUsed(alarmId: String): Int = prefs.getInt(key(alarmId, SNOOZES_USED), 0)

    fun setSnoozesUsed(alarmId: String, count: Int) {
        prefs.edit().putInt(key(alarmId, SNOOZES_USED), count.coerceAtLeast(0)).apply()
    }

    fun clearSnoozesUsed(alarmId: String) {
        prefs.edit().remove(key(alarmId, SNOOZES_USED)).apply()
    }

    private fun key(id: String, field: String) = "$id:$field"

    companion object {
        private const val PREFS = "wakehook_playback"
        private const val SOUND_ENABLED = "sound_enabled"
        private const val SOUND_URI = "sound_uri"
        private const val VIBRATION_ENABLED = "vibration_enabled"
        private const val VIBRATION_PATTERN = "vibration_pattern"
        private const val SNOOZE_ENABLED = "snooze_enabled"
        private const val SNOOZE_MINUTES = "snooze_minutes"
        private const val SNOOZE_LIMIT = "snooze_limit"
        private const val SNOOZES_USED = "snoozes_used"
        private val ALLOWED_SNOOZE_MINUTES = listOf(5, 10, 15, 30)
        private val ALLOWED_SNOOZE_LIMITS = listOf(0, 1, 3, 5)
    }
}
