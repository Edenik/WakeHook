package ai.wakehook.app.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// WakeHook brand: night-indigo ground + sunrise-amber accent (night -> morning).
private val Amber = Color(0xFFFFB74D)
private val AmberInk = Color(0xFF241500)
private val AmberContainer = Color(0xFF5A3B00)
private val Indigo = Color(0xFF8B93FF)
private val IndigoInk = Color(0xFF0C0E18)
private val Ground = Color(0xFF0E0F14)
private val Surface1 = Color(0xFF16181F)
private val SurfaceVar = Color(0xFF20232E)
private val TextHi = Color(0xFFECEEF4)
private val TextDim = Color(0xFF9CA0AC)

private val WakeHookDark = darkColorScheme(
    primary = Amber,
    onPrimary = AmberInk,
    primaryContainer = AmberContainer,
    onPrimaryContainer = Amber,
    secondary = Indigo,
    onSecondary = IndigoInk,
    secondaryContainer = Color(0xFF393022),
    onSecondaryContainer = Amber,
    outline = Color(0xFF454853),
    background = Ground,
    onBackground = TextHi,
    surface = Surface1,
    onSurface = TextHi,
    surfaceVariant = SurfaceVar,
    onSurfaceVariant = TextDim,
    outlineVariant = Color(0xFF2B2E38),
    tertiary = Color(0xFFA5D6B2),
)

@Composable
fun WakeHookTheme(content: @Composable () -> Unit) {
    // v1 is dark-theme first, branded amber.
    MaterialTheme(colorScheme = WakeHookDark, shapes = Shapes(
        extraSmall = RoundedCornerShape(12.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp)
    ), content = content)
}
