package fumi.day.literalgallery.ui.grid

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import fumi.day.literalgallery.domain.model.GridEntry
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d (EEE)", Locale.getDefault())

private const val THUMB_HEIGHT_DP = 48
private const val THUMB_TOUCH_WIDTH_DP = 24
private const val SCROLLBAR_HIDE_DELAY_MS = 800L

@Composable
fun GalleryGridScreen(
    viewModel: GalleryGridViewModel,
    onOpen: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val entries by viewModel.gridEntries.collectAsState()
    val selectedKeys by viewModel.selectedKeys.collectAsState()
    val isSelectionMode = selectedKeys.isNotEmpty()
    val trashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.clearSelection()
    }
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

    // Fast-scroll thumb, Google Photos style: a draggable indicator on the right edge
    // that appears while scrolling and fades out when idle, with a floating date bubble
    // (reusing the same month header used for the sticky overlay above) while dragging.
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thumbHeightPx = with(density) { THUMB_HEIGHT_DP.dp.toPx() }
    var trackHeightPx by remember { mutableFloatStateOf(0f) }
    var isDraggingThumb by remember { mutableStateOf(false) }
    var dragThumbY by remember { mutableFloatStateOf(0f) }

    fun maxFirstVisibleIndex(): Int {
        val visibleCount = gridState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
        return (entries.size - visibleCount).coerceAtLeast(1)
    }

    val scrollFraction by remember(entries) {
        derivedStateOf {
            (gridState.firstVisibleItemIndex.toFloat() / maxFirstVisibleIndex()).coerceIn(0f, 1f)
        }
    }

    var scrollbarVisible by remember { mutableStateOf(false) }
    LaunchedEffect(gridState.isScrollInProgress, isDraggingThumb) {
        if (gridState.isScrollInProgress || isDraggingThumb) {
            scrollbarVisible = true
        } else {
            delay(SCROLLBAR_HIDE_DELAY_MS)
            scrollbarVisible = false
        }
    }

    val canScroll = entries.size > gridState.layoutInfo.visibleItemsInfo.size

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
                    is GridEntry.Cell -> MediaGridCell(
                        item = entry.item,
                        selectionMode = isSelectionMode,
                        isSelected = entry.item.mediaKey in selectedKeys,
                        onClick = {
                            if (isSelectionMode) {
                                viewModel.toggleSelection(entry.item.mediaKey)
                            } else {
                                onOpen(entry.item.mediaKey)
                            }
                        },
                        onLongClick = { viewModel.toggleSelection(entry.item.mediaKey) }
                    )
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

        if (isSelectionMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(4.dp)
            ) {
                Text(
                    text = "${selectedKeys.size}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = { viewModel.clearSelection() }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel selection")
                }
            }
        } else {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(4.dp)
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
            }
        }

        if (isSelectionMode) {
            IconButton(
                onClick = {
                    val intentSender = viewModel.trashIntentSenderFor(selectedKeys)
                    if (intentSender != null) {
                        trashLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    } else {
                        viewModel.deleteDirectly(selectedKeys)
                        viewModel.clearSelection()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete selected",
                    tint = Color.White
                )
            }
        }

        AnimatedVisibility(
            visible = scrollbarVisible && canScroll,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = 64.dp, bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .onGloballyPositioned { trackHeightPx = it.size.height.toFloat() }
            ) {
                val maxThumbY = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)
                val thumbY = if (isDraggingThumb) dragThumbY else scrollFraction * maxThumbY

                if (isDraggingThumb) {
                    currentHeader?.let { header ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset {
                                    IntOffset(
                                        x = -(THUMB_TOUCH_WIDTH_DP.dp.roundToPx()),
                                        y = thumbY.roundToInt()
                                    )
                                }
                        ) {
                            DateBubble(header)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset { IntOffset(0, thumbY.roundToInt()) }
                        .width(THUMB_TOUCH_WIDTH_DP.dp)
                        .height(THUMB_HEIGHT_DP.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    isDraggingThumb = true
                                    dragThumbY = thumbY
                                },
                                onDragEnd = { isDraggingThumb = false },
                                onDragCancel = { isDraggingThumb = false }
                            ) { change, dragAmount ->
                                change.consume()
                                dragThumbY = (dragThumbY + dragAmount.y).coerceIn(0f, maxThumbY)
                                val fraction = if (maxThumbY > 0f) dragThumbY / maxThumbY else 0f
                                val targetIndex = (fraction * maxFirstVisibleIndex())
                                    .roundToInt()
                                    .coerceIn(0, (entries.size - 1).coerceAtLeast(0))
                                scope.launch { gridState.scrollToItem(targetIndex) }
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(4.dp)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp)
                            .background(Color.White, RoundedCornerShape(2.dp))
                    )
                }
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
private fun DateBubble(entry: GridEntry.MonthHeader) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = entry.yearMonth.atDay(1).format(monthFormatter),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
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
