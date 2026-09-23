package ai.wakehook.app.ui.onboarding

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingStateTest {
    private val app = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Test fun defaultsToNotDone() {
        assertThat(OnboardingState(app).isDone()).isFalse()
    }

    @Test fun setDone_persists() {
        OnboardingState(app).setDone()
        assertThat(OnboardingState(app).isDone()).isTrue()
    }
}
