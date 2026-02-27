package fi.pomeranssi.bookline.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE bookId = :bookId")
    fun observeById(bookId: String): Flow<BookEntity?>

    @Query(
        """
        SELECT * FROM books
        WHERE userShelves NOT LIKE '%|to-read|%'
        ORDER BY
            CASE WHEN userShelves LIKE '%|currently-reading|%' THEN 0 ELSE 1 END,
            COALESCE(userReadAt, userDateAdded) DESC
        """
    )
    fun observeTimeline(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(books: List<BookEntity>)

    @Query("DELETE FROM books WHERE lastSyncedMs < :syncTimestamp")
    suspend fun deleteNotSyncedSince(syncTimestamp: Long)
}
