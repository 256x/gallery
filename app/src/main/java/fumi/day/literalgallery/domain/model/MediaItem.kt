package fumi.day.literalgallery.domain.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val contentUri: Uri,
    val isVideo: Boolean,
    val mimeType: String,
    val dateTakenMillis: Long,
    val displayName: String,
    val width: Int,
    val height: Int,
    val durationMs: Long?,
    val sizeBytes: Long,
) {
    // Images.Media._ID and Video.Media._ID are independent autoincrement columns
    // and can collide; every lookup (nav args, pager keys) must use this, not id.
    val mediaKey: String get() = "${if (isVideo) "v" else "i"}_$id"
}
