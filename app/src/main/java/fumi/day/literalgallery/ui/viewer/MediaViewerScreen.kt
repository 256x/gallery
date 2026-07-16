package fumi.day.literalgallery.ui.viewer

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import fumi.day.literalgallery.domain.model.ExifData
import fumi.day.literalgallery.domain.model.MediaItem
import fumi.day.literalgallery.ui.grid.GalleryGridViewModel

private const val SWIPE_UP_THRESHOLD_DP = 80

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    mediaKey: String,
    viewModel: GalleryGridViewModel
) {
    val items by viewModel.filteredMediaItems.collectAsState()
    if (items.isEmpty()) return

    val initialPage = items.indexOfFirst { it.mediaKey == mediaKey }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { items.size }

    HorizontalPager(
        state = pagerState,
        key = { items[it].mediaKey },
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) { page ->
        val item = items[page]
        MediaPage(item = item, isActive = pagerState.settledPage == page, viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaPage(item: MediaItem, isActive: Boolean, viewModel: GalleryGridViewModel) {
    var showExif by remember(item.mediaKey) { mutableStateOf(false) }
    var exifData by remember(item.mediaKey) { mutableStateOf<ExifData?>(null) }
    var dragAccum by remember(item.mediaKey) { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val thresholdPx = with(density) { SWIPE_UP_THRESHOLD_DP.dp.toPx() }
    val context = LocalContext.current

    LaunchedEffect(showExif, item.mediaKey) {
        if (showExif && !item.isVideo && exifData == null) {
            exifData = viewModel.readExif(item.contentUri)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(item.mediaKey) {
                detectVerticalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onVerticalDrag = { change, amount ->
                        dragAccum += amount
                        change.consume()
                    },
                    onDragEnd = {
                        if (dragAccum < -thresholdPx) showExif = true
                    }
                )
            }
    ) {
        if (item.isVideo) {
            VideoPage(item = item, isActive = isActive)
        } else {
            PhotoPage(item = item)
        }

        IconButton(
            onClick = { shareMedia(context, item) },
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)
        ) {
            Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White)
        }
    }

    if (showExif) {
        ModalBottomSheet(
            onDismissRequest = { showExif = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            ExifPanel(item = item, exifData = exifData)
        }
    }
}

private fun shareMedia(context: Context, item: MediaItem) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = item.mimeType
        putExtra(Intent.EXTRA_STREAM, item.contentUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
