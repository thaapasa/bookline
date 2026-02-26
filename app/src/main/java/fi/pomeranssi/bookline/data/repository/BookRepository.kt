package fi.pomeranssi.bookline.data.repository

import fi.pomeranssi.bookline.data.db.BookDao
import fi.pomeranssi.bookline.data.db.BookEntity
import fi.pomeranssi.bookline.data.network.GoodreadsFeedService
import fi.pomeranssi.bookline.data.network.GoodreadsRssParser
import fi.pomeranssi.bookline.domain.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Offline-first book repository.
 *
 * Room is the single source of truth — the UI observes [observeBooks].
 * [sync] fetches all pages from the RSS feed, then replaces all rows
 * in a single transaction so removals are handled automatically.
 */
class BookRepository(
    private val bookDao: BookDao,
    private val settingsRepository: SettingsRepository,
    private val feedService: GoodreadsFeedService = GoodreadsFeedService(),
    private val rssParser: GoodreadsRssParser = GoodreadsRssParser(),
) {

    /** Observe all locally stored books as a reactive Flow. */
    fun observeBooks(): Flow<List<Book>> =
        bookDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    /** Returns true when the cached data is stale or absent. */
    fun isSyncNeeded(): Boolean = settingsRepository.isSyncStale()

    /**
     * Fetch all pages of the RSS feed and replace the local cache.
     * Returns the total number of books synced.
     */
    suspend fun sync(feedUrl: String): Int = withContext(Dispatchers.IO) {
        val allBooks = fetchAllPages(feedUrl)
        val entities = allBooks.map { BookEntity.fromDomain(it) }
        bookDao.replaceAll(entities)
        settingsRepository.lastSyncEpochMs = System.currentTimeMillis()
        allBooks.size
    }

    private suspend fun fetchAllPages(feedUrl: String): List<Book> {
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

        return allBooks
    }
}

