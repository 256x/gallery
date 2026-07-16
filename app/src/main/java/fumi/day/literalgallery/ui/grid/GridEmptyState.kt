package fumi.day.literalgallery.ui.grid

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fumi.day.literalgallery.domain.model.MediaFilter

@Composable
internal fun BoxScope.GridEmptyState(filter: MediaFilter) {
    Text(
        text = when (filter) {
            MediaFilter.ALL -> "No photos or videos yet"
            MediaFilter.PHOTOS -> "No photos"
            MediaFilter.VIDEOS -> "No videos"
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.align(Alignment.Center)
    )
}
