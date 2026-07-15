package fumi.day.literalgallery.domain.model

import java.time.LocalDate
import java.time.YearMonth

sealed class GridEntry {
    data class MonthHeader(val yearMonth: YearMonth) : GridEntry()
    data class DayLabel(val date: LocalDate) : GridEntry()
    data class Cell(val item: MediaItem) : GridEntry()
}
