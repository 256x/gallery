package fumi.day.literalgallery.data.media

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MediaDeleter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // MediaStore's trash concept (Recycle Bin) only exists from API 30. The returned
    // IntentSender must be launched by the caller so the system can show its own
    // confirmation dialog and grant write access per-item as needed.
    fun createTrashIntentSender(uris: List<Uri>): IntentSender? {
        if (uris.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val pendingIntent = MediaStore.createTrashRequest(context.contentResolver, uris, true)
        return pendingIntent.intentSender
    }

    // Best-effort fallback for API < 30, where there is no trash/recycle bin and no batch
    // delete-request flow. Deletes only succeed for files this app owns.
    fun deleteDirectly(uris: List<Uri>) {
        val resolver = context.contentResolver
        for (uri in uris) {
            runCatching { resolver.delete(uri, null, null) }
        }
    }
}
