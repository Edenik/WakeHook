package ai.wakehook.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun WakeHookTheme(content: @Composable () -> Unit) {
    // v1 is dark-theme first.
    MaterialTheme(colorScheme = darkColorScheme(), content = content)
}
