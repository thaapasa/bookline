package fi.pomeranssi.bookline.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookSortOverrideDao {

    @Query("SELECT * FROM book_sort_overrides")
    fun observeAll(): Flow<List<BookSortOverrideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: BookSortOverrideEntity)

    @Query("DELETE FROM book_sort_overrides WHERE bookId = :bookId")
    suspend fun deleteByBookId(bookId: String)

    @Query("DELETE FROM book_sort_overrides")
    suspend fun deleteAll()

    /**
     * Remove overrides for books that no longer exist in the books table.
     */
    @Query("DELETE FROM book_sort_overrides WHERE bookId NOT IN (SELECT bookId FROM books)")
    suspend fun deleteOrphans()
}
