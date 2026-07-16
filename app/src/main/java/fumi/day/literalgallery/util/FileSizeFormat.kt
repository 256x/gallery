package fumi.day.literalgallery.util

import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / 1024.0.pow(digitGroups.toDouble())
    return String.format(Locale.ENGLISH, "%.1f %s", value, units[digitGroups])
}
