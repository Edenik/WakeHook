package ai.wakehook.app.ui.onboarding

import android.content.Context

/** Tracks whether the first-run onboarding flow has been completed. */
class OnboardingState(context: Context) {
    private val prefs = context.getSharedPreferences("wakehook_onboarding", Context.MODE_PRIVATE)

    fun isDone(): Boolean = prefs.getBoolean("done", false)
    fun setDone() { prefs.edit().putBoolean("done", true).apply() }
}
