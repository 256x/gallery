package fumi.day.literalgallery.ui.viewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fumi.day.literalgallery.domain.model.ExifData
import fumi.day.literalgallery.domain.model.MediaItem
import fumi.day.literalgallery.util.formatDurationMs
import fumi.day.literalgallery.util.formatFileSize
import java.text.DateFormat
import java.util.Date

@Composable
fun ExifPanel(item: MediaItem, exifData: ExifData?) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(DateFormat.getDateTimeInstance().format(Date(item.dateTakenMillis)))
        if (item.isVideo) {
            item.durationMs?.let { Text("Duration: ${formatDurationMs(it)}") }
        } else {
            val camera = listOfNotNull(exifData?.make, exifData?.model).joinToString(" ")
            Text("Camera: ${camera.ifBlank { "—" }}")
            Text("Exposure: ${exifData?.exposureTime ?: "—"}")
            Text("ISO: ${exifData?.iso ?: "—"}")
            Text("f-number: ${exifData?.fNumber ?: "—"}")
        }
        Text("Resolution: ${item.width}x${item.height}")
        Text("Size: ${formatFileSize(item.sizeBytes)}")
    }
}
