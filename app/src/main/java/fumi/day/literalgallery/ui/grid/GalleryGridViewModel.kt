package fumi.day.literalgallery.ui.grid

import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fumi.day.literalgallery.data.exif.ExifReader
import fumi.day.literalgallery.data.media.MediaDeleter
import fumi.day.literalgallery.data.repository.MediaRepository
import fumi.day.literalgallery.domain.model.ExifData
import fumi.day.literalgallery.domain.model.GridEntry
import fumi.day.literalgallery.domain.model.MediaFilter
import fumi.day.literalgallery.domain.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

// Shared by both the grid and viewer nav destinations (see NavGraph's nested "gallery"
// graph) so the viewer reuses the already-loaded list instead of re-querying MediaStore.
@HiltViewModel
class GalleryGridViewModel @Inject constructor(
    repository: MediaRepository,
    private val exifReader: ExifReader,
    private val mediaDeleter: MediaDeleter
) : ViewModel() {

    // --- Media source, type filter, and date grouping ---

    val mediaItems: StateFlow<List<MediaItem>> = repository.observeMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _filter = MutableStateFlow(MediaFilter.ALL)
    val filter: StateFlow<MediaFilter> = _filter.asStateFlow()

    fun setFilter(filter: MediaFilter) {
        _filter.value = filter
    }

    // Filtered by type but not yet grouped into GridEntry - shared with the viewer so
    // swiping there stays within whatever subset (all/photos/videos) the grid is showing.
    val filteredMediaItems: StateFlow<List<MediaItem>> = combine(mediaItems, _filter) { items, filter ->
        when (filter) {
            MediaFilter.ALL -> items
            MediaFilter.PHOTOS -> items.filter { !it.isVideo }
            MediaFilter.VIDEOS -> items.filter { it.isVideo }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val gridEntries: StateFlow<List<GridEntry>> = filteredMediaItems
        .map(::buildGridEntries)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Multi-select and delete ---

    private val _selectedKeys = MutableStateFlow<Set<String>>(emptySet())
    val selectedKeys: StateFlow<Set<String>> = _selectedKeys.asStateFlow()

    fun toggleSelection(key: String) {
        _selectedKeys.update { current -> if (key in current) current - key else current + key }
    }

    fun clearSelection() {
        _selectedKeys.value = emptySet()
    }

    fun trashIntentSenderFor(keys: Set<String>): IntentSender? {
        val uris = mediaItems.value.filter { it.mediaKey in keys }.map { it.contentUri }
        return mediaDeleter.createTrashIntentSender(uris)
    }

    fun deleteDirectly(keys: Set<String>) {
        val uris = mediaItems.value.filter { it.mediaKey in keys }.map { it.contentUri }
        mediaDeleter.deleteDirectly(uris)
    }

    fun shareIntentFor(keys: Set<String>): Intent {
        val items = mediaItems.value.filter { it.mediaKey in keys }
        val uris = items.map { it.contentUri }
        val mimeType = when {
            items.all { !it.isVideo } -> "image/*"
            items.all { it.isVideo } -> "video/*"
            else -> "*/*"
        }
        val sendIntent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
        }
        sendIntent.type = mimeType
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(sendIntent, null)
    }

    // --- EXIF (read on demand, only when the viewer's Exif panel is opened) ---

    suspend fun readExif(uri: Uri): ExifData = withContext(Dispatchers.IO) { exifReader.read(uri) }

    private fun buildGridEntries(items: List<MediaItem>): List<GridEntry> {
        val zone = ZoneId.systemDefault()
        val entries = mutableListOf<GridEntry>()
        var lastMonth: YearMonth? = null
        var lastDay: LocalDate? = null
        for (item in items) {
            val date = Instant.ofEpochMilli(item.dateTakenMillis).atZone(zone).toLocalDate()
            val month = YearMonth.from(date)
            if (month != lastMonth) {
                entries += GridEntry.MonthHeader(month)
                lastMonth = month
                lastDay = null
            }
            if (date != lastDay) {
                entries += GridEntry.DayLabel(date)
                lastDay = date
            }
            entries += GridEntry.Cell(item)
        }
        return entries
    }
}
