package fumi.day.literalgallery.ui.grid

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fumi.day.literalgallery.data.exif.ExifReader
import fumi.day.literalgallery.data.repository.MediaRepository
import fumi.day.literalgallery.domain.model.ExifData
import fumi.day.literalgallery.domain.model.GridEntry
import fumi.day.literalgallery.domain.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val exifReader: ExifReader
) : ViewModel() {

    val mediaItems: StateFlow<List<MediaItem>> = repository.observeMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun readExif(uri: Uri): ExifData = withContext(Dispatchers.IO) { exifReader.read(uri) }

    val gridEntries: StateFlow<List<GridEntry>> = mediaItems
        .map(::buildGridEntries)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
