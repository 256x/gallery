package fumi.day.literalgallery.ui.grid

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import fumi.day.literalgallery.domain.model.GridEntry
import kotlinx.coroutines.launch

@Composable
internal fun rememberCurrentMonthHeader(
    entries: List<GridEntry>,
    gridState: LazyGridState,
    headerHeightPx: Float
): State<GridEntry.MonthHeader?> {
    // Maps each entry index to the index of the MonthHeader that governs it, so we can
    // look up "which month is currently at the top of the viewport" from a visible index.
    val headerForIndex = remember(entries) {
        val arr = IntArray(entries.size)
        var lastHeaderIdx = 0
        for (i in entries.indices) {
            if (entries[i] is GridEntry.MonthHeader) lastHeaderIdx = i
            arr[i] = lastHeaderIdx
        }
        arr
    }

    // Keyed on headerForIndex/entries: without keys, remember would create this derivedStateOf
    // only once and its closure would keep referencing the very first (often empty) entries
    // list forever, freezing the header on whatever it showed at cold start.
    return remember(headerForIndex, entries) {
        derivedStateOf {
            val visibleItems = gridState.layoutInfo.visibleItemsInfo
            // First item whose bottom edge extends below the overlay header band, i.e. the
            // first row a user can actually see uncovered - not just "first visible index",
            // which can be a row that's fully hidden behind the pinned header.
            val governingItem = visibleItems.firstOrNull { it.offset.y + it.size.height > headerHeightPx }
                ?: visibleItems.firstOrNull()
            val idx = governingItem?.index ?: return@derivedStateOf null
            val headerIdx = headerForIndex.getOrNull(idx) ?: return@derivedStateOf null
            entries.getOrNull(headerIdx) as? GridEntry.MonthHeader
        }
    }
}

@Composable
internal fun BoxScope.StickyMonthHeaderOverlay(
    header: GridEntry.MonthHeader,
    gridState: LazyGridState,
    onHeightMeasured: (Float) -> Unit
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .onGloballyPositioned { onHeightMeasured(it.size.height.toFloat()) }
            // Double-tap the pinned header to jump back to the newest items at the top.
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scope.launch { gridState.animateScrollToItem(0) }
                    }
                )
            }
    ) {
        MonthHeaderContent(header)
    }
}

@Composable
internal fun MonthHeaderContent(entry: GridEntry.MonthHeader) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Text(
            text = entry.yearMonth.atDay(1).format(monthFormatter),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
internal fun DayLabelContent(entry: GridEntry.DayLabel) {
    Text(
        text = entry.date.format(dayFormatter),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
    )
}
