package ai.wakehook.app.ui.settings

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PermissionStateTest {
    @Test fun read_returnsStatusWithoutThrowing() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val status = PermissionState.read(app)
        // On Robolectric defaults these are booleans; just assert the call is safe and typed.
        assertThat(status.exactAlarm).isAnyOf(true, false)
        assertThat(status.notifications).isAnyOf(true, false)
        assertThat(status.batteryExempt).isAnyOf(true, false)
        assertThat(status.fullScreenIntent).isAnyOf(true, false)
    }
}
