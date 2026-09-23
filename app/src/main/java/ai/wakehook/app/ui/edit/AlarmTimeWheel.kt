package ai.wakehook.app.ui.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.wakehook.app.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale

private const val wheelCycles = 3
private val wheelItemHeight = 56.dp

/** Looping, centered time wheel with touch, fling, and keyboard controls. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmTimeWheel(
    itemCount: Int,
    value: Int,
    description: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val locale = Locale.getDefault()
    val selectedIndex by remember(listState, itemCount, value) {
        derivedStateOf { centeredIndex(listState) ?: wheelCycles * itemCount + value }
    }

    LaunchedEffect(listState, itemCount, value) {
        if (listState.layoutInfo.visibleItemsInfo.isEmpty()) listState.scrollToItem(wheelCycles * itemCount + value)
    }
    LaunchedEffect(listState, itemCount) {
        snapshotFlow { centeredIndex(listState) }.distinctUntilChanged().collect { centered ->
            if (centered == null) return@collect
            val selected = Math.floorMod(centered, itemCount)
            onValueChange(selected)
            if (centered < itemCount || centered >= itemCount * 2) {
                listState.scrollToItem(wheelCycles * itemCount + selected)
            }
        }
    }

    Box(modifier.height(200.dp).semantics {
        contentDescription = description
        stateDescription = String.format(locale, "%02d", Math.floorMod(selectedIndex, itemCount))
    }) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            contentPadding = PaddingValues(vertical = 72.dp),
            modifier = Modifier.fillMaxSize().onFocusChanged { }.focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val current = Math.floorMod(selectedIndex, itemCount)
                    val next = when (event.key) {
                        Key.DirectionDown -> (current + 1) % itemCount
                        Key.DirectionUp -> Math.floorMod(current - 1, itemCount)
                        else -> return@onPreviewKeyEvent false
                    }
                    scope.launch { listState.animateScrollToItem(wheelCycles * itemCount + next) }
                    true
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(itemCount * wheelCycles, key = { it }) { index ->
                val number = Math.floorMod(index, itemCount)
                val isSelected = index == selectedIndex
                Box(Modifier.fillMaxWidth().height(wheelItemHeight).semantics { selected = isSelected }
                    .clickable { scope.launch { listState.animateScrollToItem(wheelCycles * itemCount + number) } },
                    contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Text(String.format(locale, "%02d", number),
                        fontSize = if (isSelected) 46.sp else 34.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Light,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .58f))
                }
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            MaterialTheme.colorScheme.background.copy(alpha = .95f), Color.Transparent,
            Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = .95f)))))
    }
}

private fun centeredIndex(state: androidx.compose.foundation.lazy.LazyListState): Int? {
    val layout = state.layoutInfo
    if (layout.visibleItemsInfo.isEmpty()) return null
    val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
    return layout.visibleItemsInfo.minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - viewportCenter) }?.index
}
