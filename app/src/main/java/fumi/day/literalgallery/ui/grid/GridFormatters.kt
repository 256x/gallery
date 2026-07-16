package fumi.day.literalgallery.ui.grid

import java.time.format.DateTimeFormatter
import java.util.Locale

internal val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
internal val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d (EEE)", Locale.ENGLISH)
