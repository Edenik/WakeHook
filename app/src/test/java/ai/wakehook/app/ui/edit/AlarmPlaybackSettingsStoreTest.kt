package ai.wakehook.app.ui.edit

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlarmPlaybackSettingsStoreTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val store by lazy { AlarmPlaybackSettingsStore(app) }

    @Before fun clearPreferences() {
        app.getSharedPreferences("wakehook_playback", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun defaults_keepExistingAlarmsAndGiveNewAlarmsShortSnooze() {
        assertThat(store.load("existing").snoozeMinutes)
            .isEqualTo(AlarmPlaybackSettings.EXISTING_ALARM_SNOOZE_MINUTES)
        assertThat(store.load("existing").snoozeLimit).isEqualTo(0)
        assertThat(store.load("new", isNewAlarm = true).snoozeMinutes)
            .isEqualTo(AlarmPlaybackSettings.NEW_ALARM_SNOOZE_MINUTES)
        assertThat(store.load("new", isNewAlarm = true).snoozeLimit)
            .isEqualTo(AlarmPlaybackSettings.NEW_ALARM_SNOOZE_LIMIT)
    }

    @Test fun settingsPersistPerAlarmAndNormalizeUnsupportedValues() {
        store.save("a", AlarmPlaybackSettings(soundEnabled = false, soundUri = AlarmPlaybackSettings.SILENT_SOUND,
            vibrationPattern = AlarmPlaybackSettings.STRONG, snoozeMinutes = 12, snoozeLimit = 4))

        assertThat(store.load("a").soundEnabled).isFalse()
        assertThat(store.load("a").soundUri).isEqualTo(AlarmPlaybackSettings.SILENT_SOUND)
        assertThat(store.load("a").vibrationPattern).isEqualTo(AlarmPlaybackSettings.STRONG)
        assertThat(store.load("a").snoozeMinutes).isEqualTo(10)
        assertThat(store.load("a").snoozeLimit).isEqualTo(3)
        assertThat(store.load("b").soundEnabled).isTrue()
    }

    @Test fun snoozeUsageCanIncrementAcrossRingsAndResetForNextAlarm() {
        store.setSnoozesUsed("a", 1)
        store.setSnoozesUsed("a", store.snoozesUsed("a") + 1)
        assertThat(store.snoozesUsed("a")).isEqualTo(2)
        store.clearSnoozesUsed("a")
        assertThat(store.snoozesUsed("a")).isEqualTo(0)
    }
}
