package fumi.day.literalgallery.ui.viewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

@Composable
fun PhotoPage(item: MediaItem, modifier: Modifier = Modifier) {
    var scale by remember(item.mediaKey) { mutableFloatStateOf(1f) }
    var offset by remember(item.mediaKey) { mutableStateOf(Offset.Zero) }

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
            .pointerInput(item.mediaKey) {
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        val zoomedIn = scale > 1f
                        if (event.changes.size >= 2 || zoomedIn) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            scale = (scale * zoomChange).coerceIn(1f, 5f)
                            offset = if (scale > 1f) offset + panChange else Offset.Zero
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.fastAny { it.pressed })
                }
            }
    )
}
