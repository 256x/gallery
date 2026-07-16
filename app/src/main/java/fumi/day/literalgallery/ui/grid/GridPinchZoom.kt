package fumi.day.literalgallery.ui.grid

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastAny

private const val ZOOM_IN_THRESHOLD = 1.15f
private const val ZOOM_OUT_THRESHOLD = 0.87f

// LazyVerticalGrid's own scroll gesture is a child; on the default Main pass it would
// consume single-pointer drags before this (parent) detector ever saw them, silently
// cancelling every pinch attempt. Reading on the Initial pass instead lets this run
// first, and only consuming for 2+ pointers leaves normal 1-finger scroll untouched.
internal fun Modifier.pinchToChangeColumnCount(onColumnDelta: (Int) -> Unit): Modifier =
    pointerInput(Unit) {
        var zoomAccum = 1f
        awaitEachGesture {
            awaitFirstDown(pass = PointerEventPass.Initial)
            do {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                if (event.changes.size >= 2) {
                    zoomAccum *= event.calculateZoom()
                    when {
                        zoomAccum > ZOOM_IN_THRESHOLD -> {
                            onColumnDelta(-1)
                            zoomAccum = 1f
                        }
                        zoomAccum < ZOOM_OUT_THRESHOLD -> {
                            onColumnDelta(1)
                            zoomAccum = 1f
                        }
                    }
                    event.changes.forEach { it.consume() }
                }
            } while (event.changes.fastAny { it.pressed })
            zoomAccum = 1f
        }
    }
