package fi.pomeranssi.bookline.data.network

import fi.pomeranssi.bookline.domain.model.Book
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Parses a Goodreads RSS feed into a list of [Book] domain objects.
 *
 * Uses [XmlPullParser] (available on Android without extra dependencies) for
 * low-overhead, streaming XML parsing.
 */
class GoodreadsRssParser {

    /**
     * Parse the given [inputStream] as a Goodreads RSS feed.
     * Returns the list of books found in `<item>` elements.
     */
    fun parse(inputStream: InputStream): List<Book> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        val books = mutableListOf<Book>()
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                parseItem(parser)?.let { books.add(it) }
            }
            eventType = parser.next()
        }

        return books
    }

    /**
     * Parse a single `<item>` element. The parser is positioned on the
     * opening `<item>` tag when this is called.
     */
    private fun parseItem(parser: XmlPullParser): Book? {
        var guid: String? = null
        var link: String? = null
        var title: String? = null
        var bookId: String? = null
        var authorName: String? = null
        var isbn: String? = null
        var numPages: Int? = null
        var bookPublishedYear: Int? = null
        var bookDescription: String? = null
        var imageUrl: String? = null
        var smallImageUrl: String? = null
        var mediumImageUrl: String? = null
        var largeImageUrl: String? = null
        var userRating = 0
        var averageRating: Double? = null
        var userReadAt: LocalDate? = null
        var userDateAdded: LocalDate? = null
        var userDateCreated: LocalDate? = null
        var userShelves: List<String> = emptyList()
        var userReview: String? = null
        var description: String? = null

        var depth = 1 // we're inside <item>
        var eventType = parser.next()

        while (depth > 0) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name
                    when (tag) {
                        // The nested <book id="..."> element contains <num_pages>
                        "book" -> {
                            // book id is also available as attribute here
                            numPages = parseBookElement(parser)
                        }

                        "item" -> depth++
                        else -> {
                            val text = readText(parser)
                            when (tag) {
                                "guid" -> guid = text
                                "link" -> link = text.takeIfNotBlank()
                                "title" -> title = text
                                "book_id" -> bookId = text
                                "book_image_url" -> imageUrl = text.takeIfNotBlank()
                                "book_small_image_url" -> smallImageUrl = text.takeIfNotBlank()
                                "book_medium_image_url" -> mediumImageUrl = text.takeIfNotBlank()
                                "book_large_image_url" -> largeImageUrl = text.takeIfNotBlank()
                                "book_description" -> bookDescription = text.takeIfNotBlank()
                                "author_name" -> authorName = text
                                "isbn" -> isbn = text.takeIfNotBlank()
                                "user_rating" -> userRating = text.toIntOrNull() ?: 0
                                "average_rating" -> averageRating = text.toDoubleOrNull()
                                "user_read_at" -> userReadAt = parseRssDate(text)
                                "user_date_added" -> userDateAdded = parseRssDate(text)
                                "user_date_created" -> userDateCreated = parseRssDate(text)
                                "user_shelves" -> userShelves = parseShelves(text)
                                "user_review" -> userReview = text.takeIfNotBlank()
                                "description" -> description = text.takeIfNotBlank()
                                "book_published" -> bookPublishedYear = text.toIntOrNull()
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") depth--
                }
            }
            if (depth > 0) {
                eventType = parser.next()
            }
        }

        // bookId and title are required for a valid book
        if (bookId == null || title == null) return null

        return Book(
            bookId = bookId,
            title = title,
            authorName = authorName.orEmpty(),
            isbn = isbn,
            numPages = numPages,
            bookPublishedYear = bookPublishedYear,
            bookDescription = bookDescription,
            imageUrl = imageUrl,
            smallImageUrl = smallImageUrl,
            mediumImageUrl = mediumImageUrl,
            largeImageUrl = largeImageUrl,
            userRating = userRating,
            averageRating = averageRating,
            userReadAt = userReadAt,
            userDateAdded = userDateAdded,
            userDateCreated = userDateCreated,
            userShelves = userShelves,
            userReview = userReview,
            goodreadsUrl = parseBookUrl(description) ?: link ?: guid,
        )
    }

    /**
     * Parse the nested `<book id="..."><num_pages>...</num_pages></book>` element.
     * Returns the page count (or null).
     */
    private fun parseBookElement(parser: XmlPullParser): Int? {
        var numPages: Int? = null
        var depth = 1
        var eventType = parser.next()

        while (depth > 0) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "num_pages") {
                        numPages = readText(parser).toIntOrNull()
                    } else {
                        depth++
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "book") depth--
                }
            }
            if (depth > 0) {
                eventType = parser.next()
            }
        }
        return numPages
    }

    /**
     * Read the text content of the current element (handling CDATA).
     * After this call the parser is positioned on the END_TAG.
     */
    private fun readText(parser: XmlPullParser): String {
        val sb = StringBuilder()
        var eventType = parser.next()
        while (eventType != XmlPullParser.END_TAG) {
            if (eventType == XmlPullParser.TEXT || eventType == XmlPullParser.CDSECT) {
                sb.append(parser.text)
            }
            eventType = parser.next()
        }
        return sb.toString().trim()
    }

    companion object {
        /**
         * Goodreads uses RFC 2822 dates:
         * `"Sun, 26 Jan 2025 00:00:00 +0000"` or
         * `"Sat, 21 Feb 2026 12:32:05 -0800"`
         */
        private val RSS_DATE_FORMAT = DateTimeFormatter.ofPattern(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            Locale.ENGLISH,
        )

        /**
         * Parse an RFC-2822 date string into a [LocalDate], or null if
         * the string is blank or unparseable.
         */
        internal fun parseRssDate(text: String): LocalDate? {
            if (text.isBlank()) return null
            return try {
                LocalDate.parse(text, RSS_DATE_FORMAT)
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Split a comma-separated shelf string (e.g. `"scifi, currently-reading"`)
         * into individual shelf names.
         */
        internal fun parseShelves(text: String): List<String> =
            text.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        private fun String.takeIfNotBlank(): String? =
            ifBlank { null }

        /** Regex to extract the first `<a href="...">` URL from the description HTML. */
        private val BOOK_URL_REGEX = Regex("""<a\s+href="([^"]+goodreads\.com/book/show/[^"]+)"""")

        /**
         * Extract the book URL from the item's `<description>` HTML.
         * The description contains an `<a href="...goodreads.com/book/show/...">` link.
         */
        internal fun parseBookUrl(description: String?): String? {
            if (description.isNullOrBlank()) return null
            return BOOK_URL_REGEX.find(description)?.groupValues?.get(1)
                ?.replace("&amp;", "&")
        }
    }
}

