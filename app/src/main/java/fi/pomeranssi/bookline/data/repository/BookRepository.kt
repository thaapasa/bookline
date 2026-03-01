package fi.pomeranssi.bookline.data.repository

import android.util.Log
import fi.pomeranssi.bookline.data.db.BookDao
import fi.pomeranssi.bookline.data.db.BookEntity
import fi.pomeranssi.bookline.data.db.BookSeriesDao
import fi.pomeranssi.bookline.data.db.BookSeriesEntity
import fi.pomeranssi.bookline.data.db.BookSortOverrideDao
import fi.pomeranssi.bookline.data.db.BookSortOverrideEntity
import fi.pomeranssi.bookline.data.db.SeriesInfoDao
import fi.pomeranssi.bookline.data.db.SeriesInfoEntity
import fi.pomeranssi.bookline.data.network.GoodreadsFeedService
import fi.pomeranssi.bookline.data.network.GoodreadsRssParser
import fi.pomeranssi.bookline.domain.model.Book
import fi.pomeranssi.bookline.domain.model.Series
import fi.pomeranssi.bookline.domain.model.SeriesEntry
import fi.pomeranssi.bookline.domain.model.ToReadBookItem
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
    private val seriesInfoDao: SeriesInfoDao,
    private val settingsRepository: SettingsRepository,
    private val bookSortOverrideDao: BookSortOverrideDao,
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
        combine(
            bookDao.observeTimeline(),
            bookSeriesDao.observeAll(),
        ) { entities, allSeriesRows ->
            val seriesByBookId = allSeriesRows.groupBy { it.bookId }
            entities.map { entity ->
                val entries = seriesByBookId[entity.bookId]
                    ?.map { SeriesEntry(it.seriesName, it.position) }
                    .orEmpty()
                entity.toDomain(seriesEntries = entries)
            }
        }

    private companion object {
        const val TAG = "BookRepository"
        const val MS_PER_DAY = 86_400_000L
    }

    /** Observe books on the to-read shelf, sorted by effective sort date descending. */
    fun observeToReadBooks(): Flow<List<ToReadBookItem>> =
        combine(
            bookDao.observeToRead(),
            bookSeriesDao.observeAll(),
            bookSortOverrideDao.observeAll(),
        ) { entities, allSeriesRows, overrides ->
            val overrideMap = overrides.associate { it.bookId to it.sortDateMs }
            val seriesByBookId = allSeriesRows.groupBy { it.bookId }
            entities.map { entity ->
                val entries = seriesByBookId[entity.bookId]
                    ?.map { SeriesEntry(it.seriesName, it.position) }
                    .orEmpty()
                val book = entity.toDomain(seriesEntries = entries)
                val effectiveSortDateMs = overrideMap[book.bookId]
                    ?: (book.userDateAdded?.toEpochDay()?.times(MS_PER_DAY) ?: 0L)
                ToReadBookItem(book = book, effectiveSortDateMs = effectiveSortDateMs)
            }.sortedByDescending { it.effectiveSortDateMs }
        }

    /** Update the sort date override for a to-read book. */
    suspend fun updateToReadSortDate(bookId: String, sortDateMs: Long) {
        bookSortOverrideDao.upsert(BookSortOverrideEntity(bookId, sortDateMs))
    }

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
            }.filter { it.books.isNotEmpty() }
            .sortedWith(
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

    /**
     * Observe the alternative parsed names for a series (aliases that differ from displayName).
     */
    fun observeSeriesAliases(displayName: String): Flow<List<String>> =
        seriesInfoDao.observeByDisplayName(displayName).map { entity ->
            entity?.parsedNameSet()
                ?.filter { it != displayName }
                ?.sorted()
                .orEmpty()
        }

    /** Returns true when the cached data is stale or absent. */
    fun isSyncNeeded(): Boolean {
        val stale = settingsRepository.isSyncStale()
        Log.d(TAG, "isSyncNeeded: $stale (lastSync=${settingsRepository.lastSyncEpochMs})")
        return stale
    }

    /**
     * Fetch all pages of the RSS feed and sync to the local cache.
     * Each page is upserted as soon as it is fetched so books appear
     * in the UI incrementally. After all pages are loaded, any books
     * not touched during this sync are deleted.
     * Returns the total number of books synced.
     */
    suspend fun sync(feedUrl: String): Int = withContext(Dispatchers.IO) {
        Log.i(TAG, "Sync starting from feed: ${feedUrl.take(60)}…")
        val syncTimestamp = System.currentTimeMillis()
        var totalBooks = 0
        var page = 1

        // Build parsedName → displayName map from existing series_info
        val seriesInfoMap = buildSeriesInfoMap()

        while (true) {
            val books = feedService.fetch(feedUrl, page).use { stream ->
                rssParser.parse(stream)
            }
            if (books.isEmpty()) break
            val entities = books.map { BookEntity.fromDomain(it, lastSyncedMs = syncTimestamp) }
            bookDao.upsertAll(entities)

            // Persist series entries for each book, mapping parsed names to display names
            val seriesEntities = books.flatMap { book ->
                book.seriesEntries.map { entry ->
                    val displayName = resolveSeriesDisplayName(entry.seriesName, seriesInfoMap)
                    BookSeriesEntity(
                        bookId = book.bookId,
                        seriesName = displayName,
                        position = entry.position,
                        lastSyncedMs = syncTimestamp,
                    )
                }
            }
            if (seriesEntities.isNotEmpty()) {
                bookSeriesDao.upsertAll(seriesEntities)
            }

            totalBooks += books.size
            Log.d(TAG, "Sync page $page: ${books.size} books (total so far: $totalBooks)")
            page++
        }

        if (totalBooks == 0) {
            Log.w(TAG, "Sync fetched 0 books — skipping cleanup to protect existing data")
        } else {
            bookDao.deleteNotSyncedSince(syncTimestamp)
            bookSeriesDao.deleteNotSyncedSince(syncTimestamp)
            bookSeriesDao.deleteOrphans()
            bookSortOverrideDao.deleteOrphans()
            settingsRepository.lastSyncEpochMs = syncTimestamp
            Log.i(TAG, "Sync complete: $totalBooks books in ${page - 1} pages")
        }
        totalBooks
    }

    /**
     * Rename a series. If the new name matches an existing series (by
     * displayName or parsedNames), the two are merged automatically.
     */
    suspend fun renameSeries(oldName: String, newName: String) = withContext(Dispatchers.IO) {
        if (oldName == newName) return@withContext

        val oldInfo = seriesInfoDao.getByDisplayName(oldName) ?: return@withContext
        val oldParsedNames = oldInfo.parsedNameSet()

        // Check if newName matches an existing series (by displayName or parsedNames)
        val existingByDisplay = seriesInfoDao.getByDisplayName(newName)
        val existingByParsed = if (existingByDisplay == null) {
            seriesInfoDao.findByParsedName(newName)
        } else null
        val mergeTarget = existingByDisplay ?: existingByParsed

        if (mergeTarget != null && mergeTarget.displayName != oldName) {
            // Merge: combine parsed names, keep target's display name
            val mergedParsedNames = mergeTarget.parsedNameSet() + oldParsedNames
            seriesInfoDao.upsert(
                mergeTarget.copy(
                    parsedNames = SeriesInfoEntity.encodeParsedNames(mergedParsedNames),
                ),
            )
            seriesInfoDao.delete(oldName)
            bookSeriesDao.updateSeriesName(oldName, mergeTarget.displayName)
        } else {
            // Rename in place: delete old, insert new with updated display name
            seriesInfoDao.delete(oldName)
            seriesInfoDao.upsert(
                SeriesInfoEntity(
                    displayName = newName,
                    parsedNames = SeriesInfoEntity.encodeParsedNames(oldParsedNames),
                ),
            )
            bookSeriesDao.updateSeriesName(oldName, newName)
        }
    }

    /**
     * Builds a map from each parsedName → displayName from all series_info rows.
     */
    private suspend fun buildSeriesInfoMap(): MutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        seriesInfoDao.getAll().forEach { info ->
            info.parsedNameSet().forEach { parsed -> map[parsed] = info.displayName }
        }
        return map
    }

    /**
     * Resolves a parsed series name to its display name.
     * If the parsed name is unknown, creates a new series_info row for it.
     */
    private suspend fun resolveSeriesDisplayName(
        parsedName: String,
        map: MutableMap<String, String>,
    ): String {
        map[parsedName]?.let { return it }

        // Check DB in case another sync page already created it
        val existing = seriesInfoDao.findByParsedName(parsedName)
        if (existing != null) {
            map[parsedName] = existing.displayName
            return existing.displayName
        }

        // New series — create series_info with parsedName as displayName
        seriesInfoDao.upsert(SeriesInfoEntity.forNewSeries(parsedName))
        map[parsedName] = parsedName
        return parsedName
    }
}

