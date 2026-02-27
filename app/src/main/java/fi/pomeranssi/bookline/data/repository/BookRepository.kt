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
 * [sync] fetches all pages from the RSS feed, then upserts rows by bookId
 * and removes any entries that are no longer in the feed.
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

    /** Observe a single book by its ID. */
    fun observeBook(bookId: String): Flow<Book?> =
        bookDao.observeById(bookId).map { it?.toDomain() }

    /** Observe books for the timeline, filtered and sorted at the DB level. */
    fun observeTimelineBooks(): Flow<List<Book>> =
        bookDao.observeTimeline().map { entities -> entities.map { it.toDomain() } }

    /** Observe books on the to-read shelf. */
    fun observeToReadBooks(): Flow<List<Book>> =
        bookDao.observeToRead().map { entities -> entities.map { it.toDomain() } }

    /** Returns true when the cached data is stale or absent. */
    fun isSyncNeeded(): Boolean = settingsRepository.isSyncStale()

    /**
     * Fetch all pages of the RSS feed and sync to the local cache.
     * Each page is upserted as soon as it is fetched so books appear
     * in the UI incrementally. After all pages are loaded, any books
     * not touched during this sync are deleted.
     * Returns the total number of books synced.
     */
    suspend fun sync(feedUrl: String): Int = withContext(Dispatchers.IO) {
        val syncTimestamp = System.currentTimeMillis()
        var totalBooks = 0
        var page = 1

        while (true) {
            val books = feedService.fetch(feedUrl, page).use { stream ->
                rssParser.parse(stream)
            }
            if (books.isEmpty()) break
            val entities = books.map { BookEntity.fromDomain(it, lastSyncedMs = syncTimestamp) }
            bookDao.upsertAll(entities)
            totalBooks += books.size
            page++
        }

        bookDao.deleteNotSyncedSince(syncTimestamp)
        settingsRepository.lastSyncEpochMs = syncTimestamp
        totalBooks
    }
}

