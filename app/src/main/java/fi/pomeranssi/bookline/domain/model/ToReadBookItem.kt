package fi.pomeranssi.bookline.domain.model

/**
 * A to-read book paired with its effective sort date (epoch milliseconds).
 * Used for ordering and midpoint calculation during manual reordering.
 */
data class ToReadBookItem(
    val book: Book,
    val effectiveSortDateMs: Long,
)
