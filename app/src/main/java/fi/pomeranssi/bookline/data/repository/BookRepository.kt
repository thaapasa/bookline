package fi.pomeranssi.bookline.data.repository

import fi.pomeranssi.bookline.data.db.BookDao
import fi.pomeranssi.bookline.data.db.BookEntity
import fi.pomeranssi.bookline.data.db.BookSeriesDao
import fi.pomeranssi.bookline.data.db.BookSeriesEntity
import fi.pomeranssi.bookline.data.network.GoodreadsFeedService
import fi.pomeranssi.bookline.data.network.GoodreadsRssParser
import fi.pomeranssi.bookline.domain.model.Book
import fi.pomeranssi.bookline.domain.model.Series
import fi.pomeranssi.bookline.domain.model.SeriesEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    private val bookSeriesDao: BookSeriesDao,
    private val settingsRepository: SettingsRepository,
    private val feedService: GoodreadsFeedService = GoodreadsFeedService(),
    private val rssParser: GoodreadsRssParser = GoodreadsRssParser(),
) {

    /** Observe all locally stored books as a reactive Flow. */
    fun observeBooks(): Flow<List<Book>> =
        bookDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    /** Observe a single book by its ID, including its series entries. */
    fun observeBook(bookId: String): Flow<Book?> =
        combine(
            bookDao.observeById(bookId),
            bookSeriesDao.observeAll(),
        ) { entity, allSeriesRows ->
            if (entity == null) return@combine null
            val entries = allSeriesRows
                .filter { it.bookId == bookId }
                .map { SeriesEntry(it.seriesName, it.position) }
            entity.toDomain(seriesEntries = entries)
        }

    /** Observe books for the timeline, filtered and sorted at the DB level. */
    fun observeTimelineBooks(): Flow<List<Book>> =
        bookDao.observeTimeline().map { entities -> entities.map { it.toDomain() } }

    /** Observe books on the to-read shelf. */
    fun observeToReadBooks(): Flow<List<Book>> =
        bookDao.observeToRead().map { entities -> entities.map { it.toDomain() } }

    /**
     * Observe all book series, sorted by the most recent read date descending.
     * Each [Series] contains its books (with series entries populated).
     */
    fun observeAllSeries(): Flow<List<Series>> =
        combine(
            bookDao.observeAll(),
            bookSeriesDao.observeAll(),
        ) { bookEntities, seriesRows ->
            val booksById = bookEntities.associateBy { it.bookId }

            // Group series rows by series name
            val grouped = seriesRows.groupBy { it.seriesName }

            grouped.map { (seriesName, rows) ->
                val books = rows.mapNotNull { row ->
                    val entity = booksById[row.bookId] ?: return@mapNotNull null
                    // Build series entries for this book from all its series memberships
                    val bookSeriesRows = seriesRows.filter { it.bookId == row.bookId }
                    val entries = bookSeriesRows.map { SeriesEntry(it.seriesName, it.position) }
                    entity.toDomain(seriesEntries = entries)
                }
                val lastRead = books.mapNotNull { it.userReadAt }.maxOrNull()
                Series(name = seriesName, books = books, lastReadAt = lastRead)
            }.sortedWith(
                compareByDescending<Series> { it.lastReadAt }
                    .thenBy { it.name }
            )
        }

    /**
     * Observe books in a specific series, ordered by series position.
     */
    fun observeSeriesBooks(seriesName: String): Flow<List<Book>> =
        combine(
            bookDao.observeAll(),
            bookSeriesDao.observeBySeriesName(seriesName),
        ) { bookEntities, seriesRows ->
            val booksById = bookEntities.associateBy { it.bookId }
            // All series rows for books in this series (to populate full seriesEntries)
            val allSeriesRows = seriesRows // We only have the ones for this series here

            seriesRows.mapNotNull { row ->
                val entity = booksById[row.bookId] ?: return@mapNotNull null
                entity.toDomain(
                    seriesEntries = listOf(SeriesEntry(row.seriesName, row.position)),
                )
            }.sortedBy { book ->
                book.seriesEntries.firstOrNull { it.seriesName == seriesName }?.position
                    ?: Double.MAX_VALUE
            }
        }

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

            // Persist series entries for each book
            val seriesEntities = books.flatMap { book ->
                book.seriesEntries.map { entry ->
                    BookSeriesEntity(
                        bookId = book.bookId,
                        seriesName = entry.seriesName,
                        position = entry.position,
                        lastSyncedMs = syncTimestamp,
                    )
                }
            }
            if (seriesEntities.isNotEmpty()) {
                bookSeriesDao.upsertAll(seriesEntities)
            }

            totalBooks += books.size
            page++
        }

        bookDao.deleteNotSyncedSince(syncTimestamp)
        bookSeriesDao.deleteNotSyncedSince(syncTimestamp)
        bookSeriesDao.deleteOrphans()
        settingsRepository.lastSyncEpochMs = syncTimestamp
        totalBooks
    }
}

