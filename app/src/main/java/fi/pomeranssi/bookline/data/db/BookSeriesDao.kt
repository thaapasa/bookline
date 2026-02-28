package fi.pomeranssi.bookline.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookSeriesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<BookSeriesEntity>)

    @Query("DELETE FROM book_series WHERE lastSyncedMs < :syncTimestamp")
    suspend fun deleteNotSyncedSince(syncTimestamp: Long)

    /** Delete series entries whose bookId no longer exists in the books table. */
    @Query("DELETE FROM book_series WHERE bookId NOT IN (SELECT bookId FROM books)")
    suspend fun deleteOrphans()

    /** Observe all series entries, joined with book data for building Series objects. */
    @Query(
        """
        SELECT bs.seriesName, bs.bookId, bs.position
        FROM book_series bs
        INNER JOIN books b ON bs.bookId = b.bookId
        ORDER BY bs.seriesName, bs.position
        """
    )
    fun observeAll(): Flow<List<SeriesBookRow>>

    /** Observe series entries for a single series name. */
    @Query(
        """
        SELECT bs.seriesName, bs.bookId, bs.position
        FROM book_series bs
        INNER JOIN books b ON bs.bookId = b.bookId
        WHERE bs.seriesName = :seriesName
        ORDER BY bs.position
        """
    )
    fun observeBySeriesName(seriesName: String): Flow<List<SeriesBookRow>>
}

/** Lightweight projection for series queries — just the join data. */
data class SeriesBookRow(
    val seriesName: String,
    val bookId: String,
    val position: Double,
)
