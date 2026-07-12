package fi.pomeranssi.bookline.domain.model

import java.time.LocalDate

/**
 * Reading status derived from the user's Goodreads shelves.
 */
sealed interface ReadingStatus {
    data object Read : ReadingStatus

    data object CurrentlyReading : ReadingStatus

    data object ToRead : ReadingStatus
}

/**
 * A book from the Goodreads RSS feed.
 *
 * All image URLs are optional — not every book has cover art.
 * [userReadAt] is null for books on the to-read shelf.
 */
data class Book(
    val bookId: String,
    val title: String,
    val authorName: String,
    val isbn: String?,
    val numPages: Int?,
    val bookPublishedYear: Int?,
    val bookDescription: String?,
    val imageUrl: String?,
    val smallImageUrl: String?,
    val mediumImageUrl: String?,
    val largeImageUrl: String?,
    val userRating: Int,
    val averageRating: Double?,
    val userReadAt: LocalDate?,
    val userDateAdded: LocalDate?,
    val userDateCreated: LocalDate?,
    val userShelves: List<String>,
    val userReview: String?,
    val goodreadsUrl: String?,
    val seriesEntries: List<SeriesEntry> = emptyList(),
    /** True when this book was not present in the latest successful sync. */
    val isStale: Boolean = false,
) {
    /** Derived reading status based on shelf membership. */
    val readingStatus: ReadingStatus
        get() =
            when {
                "currently-reading" in userShelves -> ReadingStatus.CurrentlyReading
                "to-read" in userShelves -> ReadingStatus.ToRead
                else -> ReadingStatus.Read
            }

    /** The best available cover image URL (largest first). */
    val bestImageUrl: String?
        get() = largeImageUrl ?: mediumImageUrl ?: imageUrl ?: smallImageUrl
}
