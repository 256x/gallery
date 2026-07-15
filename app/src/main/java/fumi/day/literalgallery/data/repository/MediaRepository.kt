package fumi.day.literalgallery.data.repository

import fumi.day.literalgallery.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun observeMedia(): Flow<List<MediaItem>>
}
