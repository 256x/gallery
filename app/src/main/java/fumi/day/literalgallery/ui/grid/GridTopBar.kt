package fumi.day.literalgallery.ui.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fumi.day.literalgallery.domain.model.MediaFilter

@Composable
internal fun BoxScope.GridTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    currentFilter: MediaFilter,
    onSetFilter: (MediaFilter) -> Unit,
    onOpenSettings: () -> Unit,
    onClearSelection: () -> Unit
) {
    if (isSelectionMode) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(4.dp)
        ) {
            Text(
                text = "$selectedCount",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = onClearSelection) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel selection")
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(4.dp)
        ) {
            IconButton(onClick = { onSetFilter(MediaFilter.ALL) }) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "All",
                    tint = filterTint(currentFilter, MediaFilter.ALL)
                )
            }
            IconButton(onClick = { onSetFilter(MediaFilter.PHOTOS) }) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Photos",
                    tint = filterTint(currentFilter, MediaFilter.PHOTOS)
                )
            }
            IconButton(onClick = { onSetFilter(MediaFilter.VIDEOS) }) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Videos",
                    tint = filterTint(currentFilter, MediaFilter.VIDEOS)
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    }
}

@Composable
private fun filterTint(current: MediaFilter, target: MediaFilter): Color =
    if (current == target) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

@Composable
internal fun BoxScope.SelectionActionButtons(onShare: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(
            onClick = onShare,
            modifier = Modifier.background(MaterialTheme.colorScheme.secondary, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share selected",
                tint = Color.White
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete selected",
                tint = Color.White
            )
        }
    }
}
