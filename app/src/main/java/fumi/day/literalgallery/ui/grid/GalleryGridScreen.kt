package fumi.day.literalgallery.ui.grid

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import fumi.day.literalgallery.domain.model.GridEntry

private const val MIN_COLUMNS = 2
private const val MAX_COLUMNS = 6

@Composable
fun GalleryGridScreen(
    viewModel: GalleryGridViewModel,
    onOpen: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val entries by viewModel.gridEntries.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()
    val selectedKeys by viewModel.selectedKeys.collectAsState()
    val isSelectionMode = selectedKeys.isNotEmpty()
    val trashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.clearSelection()
    }
    var columnCount by rememberSaveable { mutableIntStateOf(3) }
    val gridState = rememberLazyGridState()

    // The grid reserves no space for the pinned overlay header (drawn on top, not as
    // content padding), so its measured height is needed to tell which row is actually
    // visible underneath it - see rememberCurrentMonthHeader.
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    val currentHeader by rememberCurrentMonthHeader(entries, gridState, headerHeightPx)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .pinchToChangeColumnCount { delta ->
                columnCount = (columnCount + delta).coerceIn(MIN_COLUMNS, MAX_COLUMNS)
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
            StickyMonthHeaderOverlay(
                header = header,
                gridState = gridState,
                onHeightMeasured = { headerHeightPx = it }
            )
        }

        GridTopBar(
            isSelectionMode = isSelectionMode,
            selectedCount = selectedKeys.size,
            currentFilter = currentFilter,
            onSetFilter = viewModel::setFilter,
            onOpenSettings = onOpenSettings,
            onClearSelection = viewModel::clearSelection
        )

        if (isSelectionMode) {
            SelectionDeleteButton(
                onClick = {
                    val intentSender = viewModel.trashIntentSenderFor(selectedKeys)
                    if (intentSender != null) {
                        trashLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    } else {
                        viewModel.deleteDirectly(selectedKeys)
                        viewModel.clearSelection()
                    }
                }
            )
        }

        GridFastScrollbar(
            gridState = gridState,
            entries = entries,
            currentHeader = currentHeader
        )
    }
}
