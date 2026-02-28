package fi.pomeranssi.bookline.domain.model

import java.time.LocalDate

/**
 * A book series with all its known books and metadata.
 * Sorted by most-recently-read at the list level; books within
 * a series are sorted by position.
 */
data class Series(
    val name: String,
    val books: List<Book>,
    val lastReadAt: LocalDate?,
) {
    /** Cover URLs from the first 3 books (by series position) for the fan image. */
    val coverUrls: List<String>
        get() = books
            .sortedBy { book ->
                book.seriesEntries
                    .firstOrNull { it.seriesName == name }
                    ?.position ?: Double.MAX_VALUE
            }
            .mapNotNull { it.bestImageUrl }
            .take(3)

    /** Distinct author names across all books in the series. */
    val authors: List<String>
        get() = books.map { it.authorName }.distinct()
}
