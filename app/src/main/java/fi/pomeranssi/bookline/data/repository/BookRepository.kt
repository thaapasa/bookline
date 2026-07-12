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
        bookDao.observeAll().map { entities ->
            val lastSync = settingsRepository.lastSyncEpochMs
            entities.map { it.toDomain(isStale = isEntityStale(it, lastSync)) }
        }

    /** Observe a single book by its ID, including its series entries. */
    fun observeBook(bookId: String): Flow<Book?> =
        combine(
            bookDao.observeById(bookId),
            bookSeriesDao.observeAll(),
        ) { entity, allSeriesRows ->
            if (entity == null) return@combine null
            val entries =
                allSeriesRows
                    .filter { it.bookId == bookId }
                    .map { SeriesEntry(it.seriesName, it.position) }
            val lastSync = settingsRepository.lastSyncEpochMs
            entity.toDomain(seriesEntries = entries, isStale = isEntityStale(entity, lastSync))
        }

    /** Observe books for the timeline, filtered and sorted at the DB level. */
    fun observeTimelineBooks(): Flow<List<Book>> =
        combine(
            bookDao.observeTimeline(),
            bookSeriesDao.observeAll(),
        ) { entities, allSeriesRows ->
            val lastSync = settingsRepository.lastSyncEpochMs
            val seriesByBookId = allSeriesRows.groupBy { it.bookId }
            entities.map { entity ->
                val entries =
                    seriesByBookId[entity.bookId]
                        ?.map { SeriesEntry(it.seriesName, it.position) }
                        .orEmpty()
                entity.toDomain(seriesEntries = entries, isStale = isEntityStale(entity, lastSync))
            }
        }

    private companion object {
        const val TAG = "BookRepository"
        const val MS_PER_DAY = 86_400_000L

        /** Books not seen in a sync for this long are eligible for deletion. */
        const val RETENTION_PERIOD_MS = 30 * MS_PER_DAY

        /**
         * A book is "stale" when it was not present in the latest successful sync.
         * We detect this by comparing the entity's lastSyncedMs with the global
         * last-successful-sync timestamp.
         */
        fun isEntityStale(
            entity: BookEntity,
            lastSuccessfulSyncMs: Long,
        ): Boolean = lastSuccessfulSyncMs > 0 && entity.lastSyncedMs < lastSuccessfulSyncMs
    }

    /** Observe books on the to-read shelf, sorted by effective sort date descending. */
    fun observeToReadBooks(): Flow<List<ToReadBookItem>> =
        combine(
            bookDao.observeToRead(),
            bookSeriesDao.observeAll(),
            bookSortOverrideDao.observeAll(),
        ) { entities, allSeriesRows, overrides ->
            val lastSync = settingsRepository.lastSyncEpochMs
            val overrideMap = overrides.associate { it.bookId to it.sortDateMs }
            val seriesByBookId = allSeriesRows.groupBy { it.bookId }
            entities
                .map { entity ->
                    val entries =
                        seriesByBookId[entity.bookId]
                            ?.map { SeriesEntry(it.seriesName, it.position) }
                            .orEmpty()
                    val book =
                        entity.toDomain(
                            seriesEntries = entries,
                            isStale = isEntityStale(entity, lastSync),
                        )
                    val effectiveSortDateMs =
                        overrideMap[book.bookId]
                            ?: (book.userDateAdded?.toEpochDay()?.times(MS_PER_DAY) ?: 0L)
                    ToReadBookItem(book = book, effectiveSortDateMs = effectiveSortDateMs)
                }.sortedWith(
                    compareByDescending<ToReadBookItem> { it.effectiveSortDateMs }
                        .thenBy { it.book.bookId },
                )
        }

    /** Update the sort date override for a to-read book. */
    suspend fun updateToReadSortDate(
        bookId: String,
        sortDateMs: Long,
    ) {
        bookSortOverrideDao.upsert(BookSortOverrideEntity(bookId, sortDateMs))
    }

    /** Batch-update sort date overrides for multiple to-read books. */
    suspend fun updateToReadSortDates(updates: Map<String, Long>) {
        bookSortOverrideDao.upsertAll(updates.map { (id, ms) -> BookSortOverrideEntity(id, ms) })
    }

    /**
     * Immediately delete all stale books (those not present in the latest
     * successful sync). Also cleans up orphaned series entries and sort overrides.
     */
    suspend fun flushStaleBooks() =
        withContext(Dispatchers.IO) {
            val lastSync = settingsRepository.lastSyncEpochMs
            if (lastSync <= 0) {
                Log.d(TAG, "flushStaleBooks: no successful sync recorded, nothing to flush")
                return@withContext
            }
            bookDao.deleteStaleBooks(lastSync)
            bookSeriesDao.deleteStaleEntries(lastSync)
            bookSeriesDao.deleteOrphans()
            bookSortOverrideDao.deleteOrphans()
            Log.i(TAG, "Flushed stale books (lastSyncedMs < $lastSync)")
        }

    /** Observe the count of stale books (not present in the latest successful sync). */
    fun observeStaleBookCount(): Flow<Int> {
        val lastSync = settingsRepository.lastSyncEpochMs
        return if (lastSync > 0) {
            bookDao.observeStaleCount(lastSync)
        } else {
            kotlinx.coroutines.flow.flowOf(0)
        }
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
            val lastSync = settingsRepository.lastSyncEpochMs
            val booksById = bookEntities.associateBy { it.bookId }
            val seriesByBookId = seriesRows.groupBy { it.bookId }
            val grouped = seriesRows.groupBy { it.seriesName }

            grouped
                .map { (seriesName, rows) ->
                    val books =
                        rows.mapNotNull { row ->
                            val entity = booksById[row.bookId] ?: return@mapNotNull null
                            val entries =
                                seriesByBookId[row.bookId]
                                    ?.map { SeriesEntry(it.seriesName, it.position) }
                                    .orEmpty()
                            entity.toDomain(seriesEntries = entries, isStale = isEntityStale(entity, lastSync))
                        }
                    val lastRead = books.mapNotNull { it.userReadAt }.maxOrNull()
                    Series(name = seriesName, books = books, lastReadAt = lastRead)
                }.filter { it.books.isNotEmpty() }
                .sortedWith(
                    compareByDescending<Series> { it.lastReadAt }
                        .thenBy { it.name },
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
            val lastSync = settingsRepository.lastSyncEpochMs
            val booksById = bookEntities.associateBy { it.bookId }

            seriesRows
                .mapNotNull { row ->
                    val entity = booksById[row.bookId] ?: return@mapNotNull null
                    entity.toDomain(
                        seriesEntries = listOf(SeriesEntry(row.seriesName, row.position)),
                        isStale = isEntityStale(entity, lastSync),
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
            entity
                ?.parsedNameSet()
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
     * in the UI incrementally. After all pages are loaded, books not
     * seen for longer than the retention period are removed.
     *
     * **Dormancy protection:** if the last successful sync was more than
     * [RETENTION_PERIOD_MS] ago, all existing books' timestamps are
     * refreshed first so that they are not immediately deleted.
     *
     * Returns the total number of books synced.
     */
    suspend fun sync(feedUrl: String): Int =
        withContext(Dispatchers.IO) {
            Log.i(TAG, "Sync starting from feed: ${feedUrl.take(60)}…")
            val syncTimestamp = System.currentTimeMillis()
            var totalBooks = 0
            var page = 1

            val lastSuccessfulSync = settingsRepository.lastSyncEpochMs
            val isDormant =
                lastSuccessfulSync > 0 &&
                    syncTimestamp - lastSuccessfulSync > RETENTION_PERIOD_MS
            var dormancyHandled = false

            // Build parsedName → displayName map from existing series_info
            val seriesInfoMap = buildSeriesInfoMap()

            while (true) {
                val books =
                    feedService.fetch(feedUrl, page).use { stream ->
                        rssParser.parse(stream)
                    }
                if (books.isEmpty()) break

                // Dormancy protection: defer until we've seen a valid page-1 response,
                // so a transient empty/failed fetch doesn't trigger a needless DB rewrite.
                if (isDormant && !dormancyHandled) {
                    Log.i(
                        TAG,
                        "Dormancy detected (last sync ${(syncTimestamp - lastSuccessfulSync) / MS_PER_DAY} days ago) — refreshing retention timestamps",
                    )
                    bookDao.refreshLastSyncedMs(syncTimestamp)
                    bookSeriesDao.refreshLastSyncedMs(syncTimestamp)
                    dormancyHandled = true
                }

                val entities = books.map { BookEntity.fromDomain(it, lastSyncedMs = syncTimestamp) }
                bookDao.upsertAll(entities)

                // Persist series entries for each book, mapping parsed names to display names
                val seriesEntities =
                    books.flatMap { book ->
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
                // Empty page-1 almost always means transient failure (network, auth,
                // rate-limit), not a legitimately empty Goodreads shelf. Throw so the
                // caller treats it as an error rather than a fresh success that would
                // suppress further retries for the next 24h.
                throw EmptyFeedException()
            }
            val retentionThreshold = syncTimestamp - RETENTION_PERIOD_MS
            bookDao.deleteStaleBooks(retentionThreshold)
            bookSeriesDao.deleteStaleEntries(retentionThreshold)
            bookSeriesDao.deleteOrphans()
            bookSortOverrideDao.deleteOrphans()
            settingsRepository.lastSyncEpochMs = syncTimestamp
            Log.i(TAG, "Sync complete: $totalBooks books in ${page - 1} pages")
            totalBooks
        }

    class EmptyFeedException : RuntimeException("Feed returned no books")

    /**
     * Rename a series. If the new name matches an existing series (by
     * displayName or parsedNames), the two are merged automatically.
     */
    suspend fun renameSeries(
        oldName: String,
        newName: String,
    ) = withContext(Dispatchers.IO) {
        if (oldName == newName) return@withContext

        val oldInfo = seriesInfoDao.getByDisplayName(oldName) ?: return@withContext
        val oldParsedNames = oldInfo.parsedNameSet()

        // Check if newName matches an existing series (by displayName or parsedNames)
        val existingByDisplay = seriesInfoDao.getByDisplayName(newName)
        val existingByParsed =
            if (existingByDisplay == null) {
                seriesInfoDao.findByParsedName(newName)
            } else {
                null
            }
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
