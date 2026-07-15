package fumi.day.literalgallery.data.media

import fumi.day.literalgallery.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

// Extensibility seam: a future remote/opencloud source is a new MediaSource
// implementation bound with @IntoSet — no change needed to the repository, ViewModel, or UI.
interface MediaSource {
    fun observe(): Flow<List<MediaItem>>
}
