package fi.pomeranssi.bookline.ui.common

import fi.pomeranssi.bookline.domain.model.Book
import fi.pomeranssi.bookline.domain.model.Series
import fi.pomeranssi.bookline.domain.model.SeriesEntry
import fi.pomeranssi.bookline.domain.model.ToReadBookItem
import java.time.LocalDate

/**
 * Shared fixture data for `@Preview` composables. Never used at runtime.
 *
 * All books, authors, and series are fictional. Cover images resolve via the
 * `preview://` scheme to drawables in `src/debug/res/drawable-nodpi/` (see
 * `BookCover`), so neither the fixtures' images nor this data reaches the
 * release APK.
 *
 * Base books cover the three reading statuses; tweak per-preview details
 * with `copy()` at the call site.
 */
internal object PreviewData {

    private const val SERIES_NAME = "The Horizon Cycle"

    val bookRead = Book(
        bookId = "preview-1",
        title = "Beyond the Horizon",
        authorName = "A. J. Morgan",
        isbn = null,
        numPages = 432,
        bookPublishedYear = 2019,
        bookDescription = "Kael Draven swore he would never return to the ruins of " +
            "Thornspire Keep. But when the river valleys fall silent and the last " +
            "caravan out of the borderlands vanishes, he straps on his father's sword " +
            "and climbs toward the citadel one final time.<br><br><i>Every journey " +
            "leaves a mark.</i> Some are carved in stone. <b>His</b> may be carved in " +
            "the world itself.",
        imageUrl = "preview://preview_cover_beyond_horizon",
        smallImageUrl = null,
        mediumImageUrl = null,
        largeImageUrl = null,
        userRating = 4,
        averageRating = 4.32,
        userReadAt = LocalDate.of(2025, 12, 15),
        userDateAdded = LocalDate.of(2025, 10, 1),
        userDateCreated = LocalDate.of(2025, 10, 1),
        userShelves = listOf("read", "fantasy"),
        userReview = null,
        goodreadsUrl = "https://example.com/books/beyond-the-horizon",
        seriesEntries = listOf(
            SeriesEntry(seriesName = SERIES_NAME, position = 1.0),
        ),
    )

    val bookCurrentlyReading = bookRead.copy(
        bookId = "preview-2",
        title = "The Silent Valleys",
        numPages = 518,
        bookPublishedYear = 2021,
        bookDescription = null,
        imageUrl = "preview://preview_cover_silent_valleys",
        userRating = 0,
        averageRating = 4.41,
        userReadAt = null,
        userShelves = listOf("currently-reading"),
        goodreadsUrl = null,
        seriesEntries = listOf(
            SeriesEntry(seriesName = SERIES_NAME, position = 2.0),
        ),
    )

    /** Unannounced: no ISBN, page count, year, or cover. */
    val bookToRead = bookRead.copy(
        bookId = "preview-3",
        title = "Crown of Thornspire",
        numPages = null,
        bookPublishedYear = null,
        bookDescription = null,
        imageUrl = null,
        userRating = 0,
        averageRating = null,
        userReadAt = null,
        userShelves = listOf("to-read"),
        goodreadsUrl = null,
        seriesEntries = listOf(
            SeriesEntry(seriesName = SERIES_NAME, position = 3.0),
        ),
    )

    val books = listOf(bookRead, bookCurrentlyReading, bookToRead)

    val series = Series(
        name = SERIES_NAME,
        books = books,
        lastReadAt = LocalDate.of(2025, 12, 15),
    )

    /** Default sort date = date added, matching the to-read override scheme. */
    private fun sortDateMs(date: LocalDate) = date.toEpochDay() * 86_400_000L

    val toReadItems = listOf(
        // Book 2 of The Horizon Cycle as to-read so the list shows a cover image
        ToReadBookItem(
            book = bookCurrentlyReading.copy(userShelves = listOf("to-read")),
            effectiveSortDateMs = sortDateMs(LocalDate.of(2025, 10, 1)),
        ),
        ToReadBookItem(
            book = bookRead.copy(
                bookId = "preview-5",
                title = "The Ashen Road",
                authorName = "E. V. Hale",
                numPages = 388,
                bookPublishedYear = 2015,
                bookDescription = null,
                imageUrl = null,
                userRating = 0,
                averageRating = 4.05,
                userReadAt = null,
                userShelves = listOf("to-read"),
                goodreadsUrl = null,
                seriesEntries = listOf(
                    SeriesEntry(seriesName = "The Ashen Roads", position = 1.0),
                ),
            ),
            effectiveSortDateMs = sortDateMs(LocalDate.of(2025, 9, 12)),
        ),
        ToReadBookItem(
            book = bookRead.copy(
                bookId = "preview-6",
                title = "The Hollow Crossing",
                numPages = 356,
                bookPublishedYear = 2023,
                bookDescription = null,
                imageUrl = null,
                userRating = 0,
                averageRating = 3.98,
                userReadAt = null,
                userShelves = listOf("to-read"),
                goodreadsUrl = null,
                seriesEntries = emptyList(),
            ),
            effectiveSortDateMs = sortDateMs(LocalDate.of(2025, 8, 3)),
        ),
    )

    val seriesList = listOf(
        series,
        Series(
            name = "The Ashen Roads",
            books = listOf(
                bookRead.copy(
                    bookId = "preview-4",
                    title = "The Ashen Road",
                    authorName = "E. V. Hale",
                    numPages = 388,
                    bookPublishedYear = 2015,
                    bookDescription = null,
                    imageUrl = null,
                    goodreadsUrl = null,
                    seriesEntries = listOf(
                        SeriesEntry(seriesName = "The Ashen Roads", position = 1.0),
                    ),
                ),
            ),
            lastReadAt = LocalDate.of(2025, 11, 2),
        ),
    )
}
