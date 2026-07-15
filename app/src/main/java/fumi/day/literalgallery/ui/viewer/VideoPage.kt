package fumi.day.literalgallery.ui.viewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import fumi.day.literalgallery.domain.model.MediaItem

// A viewer-scoped ExoPlayer, not a background/foreground service — unlike LiteralPlayer,
// this app never plays after the viewer page leaves composition.
@Composable
fun VideoPage(item: MediaItem, isActive: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(item.mediaKey) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(Media3Item.fromUri(item.contentUri))
            prepare()
        }
    }

    DisposableEffect(item.mediaKey) {
        onDispose { player.release() }
    }

    LaunchedEffect(isActive) {
        if (isActive) player.play() else player.pause()
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = {
            PlayerView(it).apply {
                this.player = player
                useController = true
            }
        }
    )
}
