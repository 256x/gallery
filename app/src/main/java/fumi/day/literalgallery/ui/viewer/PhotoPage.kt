package fumi.day.literalgallery.ui.viewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.util.fastAny
import coil3.compose.AsyncImage
import fumi.day.literalgallery.domain.model.MediaItem

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_TIMEOUT_MS = 300L
private const val TAP_SLOP_PX = 18f

@Composable
fun PhotoPage(item: MediaItem, modifier: Modifier = Modifier) {
    var scale by remember(item.mediaKey) { mutableFloatStateOf(MIN_SCALE) }
    var offset by remember(item.mediaKey) { mutableStateOf(Offset.Zero) }
    var lastTapTime by remember(item.mediaKey) { mutableLongStateOf(0L) }

    AsyncImage(
        model = item.contentUri,
        contentDescription = item.displayName,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
            // This composable is the innermost pointer input node (child of the page-swipe
            // pager and the swipe-up-for-exif detector above it), so on the default Main
            // pass it gets first look at every event. Consuming only for a pinch (2+
            // pointers) or while already zoomed in leaves plain 1-finger swipes/drags
            // unconsumed so the pager and exif gesture above still see them normally.
            // Double-tap is detected in this same loop (via cumulative pan distance across
            // the gesture) rather than a second pointerInput, since a separate
            // detectTapGestures would race this one for event consumption order.
            .pointerInput(item.mediaKey) {
                awaitEachGesture {
                    var multiTouch = false
                    var totalPan = Offset.Zero
                    do {
                        val event = awaitPointerEvent()
                        val zoomedIn = scale > MIN_SCALE
                        if (event.changes.size >= 2) multiTouch = true
                        val panChange = event.calculatePan()
                        totalPan += panChange
                        if (event.changes.size >= 2 || zoomedIn) {
                            val zoomChange = event.calculateZoom()
                            scale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
                            offset = if (scale > MIN_SCALE) offset + panChange else Offset.Zero
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.fastAny { it.pressed })

                    val wasTap = !multiTouch &&
                        totalPan.x * totalPan.x + totalPan.y * totalPan.y < TAP_SLOP_PX * TAP_SLOP_PX
                    if (wasTap) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < DOUBLE_TAP_TIMEOUT_MS) {
                            lastTapTime = 0L
                            if (scale > MIN_SCALE) {
                                scale = MIN_SCALE
                                offset = Offset.Zero
                            } else {
                                scale = MAX_SCALE
                            }
                        } else {
                            lastTapTime = now
                        }
                    }
                }
            }
    )
}
