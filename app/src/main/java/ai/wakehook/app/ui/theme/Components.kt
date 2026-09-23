package ai.wakehook.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.wakehook.app.R

val ReadyGreen = Color(0xFFA5D6B2)
val Sunrise = Brush.linearGradient(listOf(Color(0xFF332A22), Color(0xFF211E20)))

@Composable
fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
}

@Composable
fun DesignCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun ClockTime(text: String, modifier: Modifier = Modifier, large: Boolean = false, accent: Boolean = false) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(text, modifier = modifier, fontSize = if (large) 72.sp else 44.sp,
            lineHeight = if (large) 80.sp else 52.sp, fontWeight = FontWeight.Light,
            letterSpacing = (-2).sp, color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun AlarmEmblem(modifier: Modifier = Modifier) {
    Box(modifier.size(128.dp).background(Sunrise, CircleShape), contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.ic_wakehook), null, Modifier.size(88.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)? = null, showBrand: Boolean = false, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (showBrand) Icon(painterResource(R.drawable.ic_wakehook), null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.headlineSmall)
        }
    },
        navigationIcon = { if (onBack != null) IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
        } }, actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
}
