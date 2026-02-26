package fi.pomeranssi.bookline.data.repository

import fi.pomeranssi.bookline.data.network.GoodreadsFeedService
import fi.pomeranssi.bookline.data.network.GoodreadsRssParser
import fi.pomeranssi.bookline.domain.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provides access to the user's book list.
 *
 * Fetches all pages of the Goodreads RSS feed and parses them in-memory.
 * A Room cache can be layered in later for offline-first support.
 */
class BookRepository(
    private val feedService: GoodreadsFeedService = GoodreadsFeedService(),
    private val rssParser: GoodreadsRssParser = GoodreadsRssParser(),
) {

    /**
     * Fetch and parse **all** books from the given RSS [feedUrl],
     * paginating automatically until an empty page is returned.
     */
    suspend fun getBooks(feedUrl: String): List<Book> = withContext(Dispatchers.IO) {
        val allBooks = mutableListOf<Book>()
        var page = 1

        while (true) {
            val books = feedService.fetch(feedUrl, page).use { stream ->
                rssParser.parse(stream)
            }
            if (books.isEmpty()) break
            allBooks.addAll(books)
            page++
        }

        allBooks
    }
}

