package fi.pomeranssi.bookline.domain.model

/**
 * A single series membership for a book, parsed from the Goodreads title.
 * A book can belong to multiple series (e.g. Discworld + City Watch).
 */
data class SeriesEntry(
    val seriesName: String,
    val position: Double,
)
