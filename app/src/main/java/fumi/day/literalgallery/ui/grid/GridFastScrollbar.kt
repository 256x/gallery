package fumi.day.literalgallery.ui.grid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fumi.day.literalgallery.domain.model.GridEntry
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val THUMB_HEIGHT_DP = 48
private const val THUMB_TOUCH_WIDTH_DP = 24
private const val SCROLLBAR_HIDE_DELAY_MS = 800L

// Google Photos style: a draggable thumb on the right edge that appears while scrolling
// and fades out when idle, with a floating month/year bubble while dragging.
@Composable
internal fun BoxScope.GridFastScrollbar(
    gridState: LazyGridState,
    entries: List<GridEntry>,
    currentHeader: GridEntry.MonthHeader?
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thumbHeightPx = with(density) { THUMB_HEIGHT_DP.dp.toPx() }
    var trackHeightPx by remember { mutableFloatStateOf(0f) }
    var isDraggingThumb by remember { mutableStateOf(false) }
    var dragThumbY by remember { mutableFloatStateOf(0f) }
    var scrollbarVisible by remember { mutableStateOf(false) }

    fun maxFirstVisibleIndex(): Int {
        val visibleCount = gridState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
        return (entries.size - visibleCount).coerceAtLeast(1)
    }

    val scrollFraction = (gridState.firstVisibleItemIndex.toFloat() / maxFirstVisibleIndex()).coerceIn(0f, 1f)

    LaunchedEffect(gridState.isScrollInProgress, isDraggingThumb) {
        if (gridState.isScrollInProgress || isDraggingThumb) {
            scrollbarVisible = true
        } else {
            delay(SCROLLBAR_HIDE_DELAY_MS)
            scrollbarVisible = false
        }
    }

    val canScroll = entries.size > gridState.layoutInfo.visibleItemsInfo.size

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

@Composable
internal fun DateBubble(entry: GridEntry.MonthHeader) {
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
