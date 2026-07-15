package fumi.day.literalgallery.data.exif

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import fumi.day.literalgallery.domain.model.ExifData
import javax.inject.Inject

class ExifReader @Inject constructor(private val context: Context) {

    fun read(uri: Uri): ExifData {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                ExifData(
                    make = exif.getAttribute(ExifInterface.TAG_MAKE),
                    model = exif.getAttribute(ExifInterface.TAG_MODEL),
                    exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME),
                    iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                        ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS),
                    fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER),
                )
            } ?: ExifData()
        } catch (e: Exception) {
            // Many images have no/partial EXIF (screenshots, stripped metadata) — missing
            // fields render as "—" in the UI rather than crashing the viewer.
            ExifData()
        }
    }
}
