package fi.pomeranssi.bookline.data.repository

import fi.pomeranssi.bookline.data.network.GoodreadsFeedService
import fi.pomeranssi.bookline.data.network.GoodreadsRssParser
import fi.pomeranssi.bookline.domain.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provides access to the user's book list.
 *
 * Currently fetches directly from the Goodreads RSS feed and parses in-memory.
 * A Room cache can be layered in later for offline-first support.
 */
class BookRepository(
    private val feedService: GoodreadsFeedService = GoodreadsFeedService(),
    private val rssParser: GoodreadsRssParser = GoodreadsRssParser(),
) {

    /**
     * Fetch and parse all books from the given RSS [feedUrl].
     */
    suspend fun getBooks(feedUrl: String): List<Book> = withContext(Dispatchers.IO) {
        feedService.fetch(feedUrl).use { stream ->
            rssParser.parse(stream)
        }
    }
}

