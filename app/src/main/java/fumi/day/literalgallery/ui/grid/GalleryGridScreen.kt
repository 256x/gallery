package fumi.day.literalgallery.ui.grid

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import fumi.day.literalgallery.domain.model.GridEntry
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d (EEE)", Locale.getDefault())

@Composable
fun GalleryGridScreen(
    viewModel: GalleryGridViewModel,
    onOpen: (String) -> Unit
) {
    val entries by viewModel.gridEntries.collectAsState()
    var columnCount by rememberSaveable { mutableIntStateOf(3) }
    var zoomAccum by remember { mutableFloatStateOf(1f) }
    val gridState = rememberLazyGridState()

    // Maps each entry index to the index of the MonthHeader that governs it, so the
    // overlay can look up "which month is currently at the top of the viewport".
    val headerForIndex = remember(entries) {
        val arr = IntArray(entries.size)
        var lastHeaderIdx = 0
        for (i in entries.indices) {
            if (entries[i] is GridEntry.MonthHeader) lastHeaderIdx = i
            arr[i] = lastHeaderIdx
        }
        arr
    }

    // Measured height of the pinned overlay header. The grid itself reserves no space for
    // it (it's drawn on top, not as content padding), so gridState.firstVisibleItemIndex
    // alone would still report a row that's actually hidden entirely behind the overlay.
    var headerHeightPx by remember { mutableFloatStateOf(0f) }

    // Keyed on headerForIndex/entries: without keys, remember would create this derivedStateOf
    // only once and its closure would keep referencing the very first (often empty) entries
    // list forever, freezing the header on whatever it showed at cold start.
    val currentHeader by remember(headerForIndex, entries) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            // LazyVerticalGrid's own scroll gesture is a child; on the default Main pass it
            // would consume single-pointer drags before this (parent) detector ever saw them,
            // silently cancelling every pinch attempt. Reading on the Initial pass instead lets
            // this run first, and only consuming for 2+ pointers leaves normal 1-finger scroll
            // untouched.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        if (event.changes.size >= 2) {
                            zoomAccum *= event.calculateZoom()
                            when {
                                zoomAccum > 1.15f -> {
                                    columnCount = (columnCount - 1).coerceIn(2, 6)
                                    zoomAccum = 1f
                                }
                                zoomAccum < 0.87f -> {
                                    columnCount = (columnCount + 1).coerceIn(2, 6)
                                    zoomAccum = 1f
                                }
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.fastAny { it.pressed })
                    zoomAccum = 1f
                }
            }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            state = gridState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = entries.size,
                key = { index ->
                    when (val entry = entries[index]) {
                        is GridEntry.MonthHeader -> "month_${entry.yearMonth}"
                        is GridEntry.DayLabel -> "day_${entry.date}"
                        is GridEntry.Cell -> entry.item.mediaKey
                    }
                },
                span = { index ->
                    if (entries[index] is GridEntry.Cell) GridItemSpan(1) else GridItemSpan(maxLineSpan)
                }
            ) { index ->
                when (val entry = entries[index]) {
                    is GridEntry.MonthHeader -> MonthHeaderContent(entry)
                    is GridEntry.DayLabel -> DayLabelContent(entry)
                    is GridEntry.Cell -> MediaGridCell(entry.item, onClick = { onOpen(entry.item.mediaKey) })
                }
            }
        }

        currentHeader?.let { header ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .onGloballyPositioned { headerHeightPx = it.size.height.toFloat() }
            ) {
                MonthHeaderContent(header)
            }
        }
    }
}

@Composable
private fun MonthHeaderContent(entry: GridEntry.MonthHeader) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Text(
            text = entry.yearMonth.atDay(1).format(monthFormatter),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun DayLabelContent(entry: GridEntry.DayLabel) {
    Text(
        text = entry.date.format(dayFormatter),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
    )
}
