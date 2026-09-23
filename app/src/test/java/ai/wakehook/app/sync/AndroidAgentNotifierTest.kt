package ai.wakehook.app.sync

import android.app.Application
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import ai.wakehook.app.domain.Alarm
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AndroidAgentNotifierTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val nm = app.getSystemService(NotificationManager::class.java)
    private val notifier = AndroidAgentNotifier(app)

    @Test
    fun oneAddedAlarm_postsExactlyOneNotificationOnAgentActivityChannel() {
        val alarm = Alarm(hour = 6, minute = 45, label = "Gym", source = "claude")

        notifier.notifyAgentActivity(added = listOf(alarm), changed = emptyList())

        val posted = shadowOf(nm).allNotifications
        assertThat(posted).hasSize(1)
        assertThat(posted[0].channelId).isEqualTo("agent_activity")
    }

    @Test
    fun emptyAddedAndChanged_postsNothing() {
        notifier.notifyAgentActivity(added = emptyList(), changed = emptyList())

        assertThat(shadowOf(nm).allNotifications).isEmpty()
    }
}
