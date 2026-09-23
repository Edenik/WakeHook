package ai.wakehook.app.alarm

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ai.wakehook.app.data.AlarmDatabase
import ai.wakehook.app.data.RoomAlarmRepository
import ai.wakehook.app.sync.SyncTrigger
import ai.wakehook.app.ui.edit.AlarmPlaybackSettings
import ai.wakehook.app.ui.edit.AlarmPlaybackSettingsStore
import ai.wakehook.app.ui.theme.WakeHookTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RingActivity : ComponentActivity() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Backed by Compose state so a takeover from onNewIntent (second alarm ringing while
    // this screen is already showing) recomposes the UI with the newest alarm's id/label/time.
    private var currentId by mutableStateOf("")
    private var currentLabel by mutableStateOf("")
    private var currentTime by mutableStateOf("")
    private var playback by mutableStateOf(AlarmPlaybackSettings())
    private var snoozesUsed by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF0E0F14.toInt()
        window.navigationBarColor = 0xFF0E0F14.toInt()
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        (getSystemService(KEYGUARD_SERVICE) as? KeyguardManager)
            ?.requestDismissKeyguard(this, null)

        loadFromIntent(intent)
        acquireWakeLock()
        startRinging()

        setContent {
            WakeHookTheme {
                RingScreen(
                    time = currentTime,
                    label = currentLabel,
                    playback = playback,
                    snoozesUsed = snoozesUsed,
                    onDismiss = { stopAll(currentId); finish() },
                    onSnooze = {
                        val id = currentId
                        try {
                            val used = snoozesUsed + 1
                            val snoozedUntil = System.currentTimeMillis() + playback.snoozeMinutes * 60 * 1000L
                            AlarmScheduler(this).scheduleSnooze(id, currentLabel, snoozedUntil)
                            AlarmPlaybackSettingsStore(this).setSnoozesUsed(id, used)
                            snoozesUsed = used
                            persistSnooze(id, snoozedUntil)
                            stopAll(currentId); finish()
                        } catch (_: SecurityException) {
                            android.widget.Toast.makeText(this, getString(ai.wakehook.app.R.string.save_schedule_failed), android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                )
            }
        }
    }

    /**
     * `RingActivity` is `singleInstance`, so a second alarm firing while this screen is
     * already showing does NOT re-run onCreate — the system redelivers here instead. The
     * newest alarm takes over the ring screen: stop the current sound/vibration, cancel the
     * *previous* alarm's ongoing notification (its own notification stays posted until this
     * newest alarm is dismissed/snoozed), then start ringing for the new alarm. Dismiss/snooze
     * always act on `currentId`, which this updates to the newest alarm.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val previousId = currentId
        stopRinging()
        if (previousId.isNotEmpty())
            (getSystemService(NOTIFICATION_SERVICE) as? NotificationManager)
                ?.cancel(AlarmIntents.requestCode(previousId))

        loadFromIntent(intent)
        acquireWakeLock()
        startRinging()
    }

    /**
     * Persists the snooze on the alarm itself (`snoozedUntil` + a version bump so this local
     * change wins the next merge) and kicks off a sync so agents/other views see it. Runs off
     * the main thread; if the alarm no longer exists (e.g. deleted mid-ring) it just proceeds.
     */
    private fun persistSnooze(id: String, snoozedUntil: Long) {
        val appContext = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val repo = RoomAlarmRepository(AlarmDatabase.get(appContext).alarmDao())
            val alarm = repo.get(id)
            if (alarm != null) {
                repo.upsert(alarm.copy(snoozedUntil = snoozedUntil, version = alarm.version + 1))
            }
            SyncTrigger.now(appContext)
        }
    }

    private fun loadFromIntent(intent: Intent) {
        currentId = intent.getStringExtra(AlarmIntents.EXTRA_ID) ?: ""
        currentLabel = intent.getStringExtra(AlarmIntents.EXTRA_LABEL) ?: ""
        currentTime = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val settings = AlarmPlaybackSettingsStore(this)
        if (!intent.getBooleanExtra(AlarmIntents.EXTRA_SNOOZE, false)) settings.clearSnoozesUsed(currentId)
        playback = settings.load(currentId)
        snoozesUsed = settings.snoozesUsed(currentId)
    }

    /** Guarded against double-acquire so a takeover never leaks/re-acquires a held wakelock. */
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wakehook:ring").also {
            it.acquire(10 * 60 * 1000L)
        }
    }

    private fun startRinging() {
        try {
            val soundUri = when {
                !playback.soundEnabled || playback.soundUri == AlarmPlaybackSettings.SILENT_SOUND -> null
                playback.soundUri == AlarmPlaybackSettings.SYSTEM_DEFAULT_SOUND || playback.soundUri == null ->
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                else -> android.net.Uri.parse(playback.soundUri)
            }
            if (soundUri != null) {
                player = MediaPlayer().apply {
                    setDataSource(this@RingActivity, soundUri)
                    setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM).build())
                    isLooping = true
                    prepare()
                    start()
                }
            }
        } catch (_: Exception) {}
        if (playback.vibrationEnabled) vibrator = (getSystemService(VIBRATOR_SERVICE) as? Vibrator)?.also {
            val pattern = when (playback.vibrationPattern) {
                AlarmPlaybackSettings.GENTLE -> longArrayOf(0, 180, 500)
                AlarmPlaybackSettings.STRONG -> longArrayOf(0, 800, 250)
                else -> longArrayOf(0, 600, 400)
            }
            it.vibrate(VibrationEffect.createWaveform(pattern, 0))
        }
    }

    /** Stops sound/vibration only. Does NOT touch the wakelock or any notification. */
    private fun stopRinging() {
        try { player?.stop(); player?.release() } catch (_: Exception) {}
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun stopAll(id: String) {
        stopRinging()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        if (id.isNotEmpty())
            (getSystemService(NOTIFICATION_SERVICE) as? NotificationManager)
                ?.cancel(AlarmIntents.requestCode(id))
    }

    override fun onDestroy() { stopAll(currentId); super.onDestroy() }
    @Deprecated("force explicit choice") override fun onBackPressed() { /* ignore */ }
}
