package fi.pomeranssi.bookline.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores a manual sort date override for a to-read book.
 * When present, this value is used instead of [BookEntity.userDateAdded]
 * to determine the book's position in the to-read list.
 */
@Entity(tableName = "book_sort_overrides")
data class BookSortOverrideEntity(
    @PrimaryKey val bookId: String,
    val sortDateMs: Long,
)
