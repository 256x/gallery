package fumi.day.literalgallery.data.repository

import fumi.day.literalgallery.data.media.MediaSource
import fumi.day.literalgallery.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards MediaSource>
) : MediaRepository {
    override fun observeMedia(): Flow<List<MediaItem>> =
        combine(sources.map { it.observe() }) { lists ->
            lists.flatMap { it }.sortedByDescending { it.dateTakenMillis }
        }
}
