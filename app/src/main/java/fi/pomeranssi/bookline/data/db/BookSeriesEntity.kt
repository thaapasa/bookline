package fi.pomeranssi.bookline.data.db

import androidx.room.Entity
import androidx.room.Index

/**
 * Room entity representing a book's membership in a series.
 * One row per book-series pair — a book can belong to multiple series.
 */
@Entity(
    tableName = "book_series",
    primaryKeys = ["bookId", "seriesName"],
    indices = [Index(value = ["seriesName"])],
)
data class BookSeriesEntity(
    val bookId: String,
    val seriesName: String,
    val position: Double,
    val lastSyncedMs: Long = 0,
)
