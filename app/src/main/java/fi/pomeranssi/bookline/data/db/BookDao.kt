package fi.pomeranssi.bookline.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("DELETE FROM books")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<BookEntity>)

    /** Replace all stored books in a single transaction. */
    @Transaction
    suspend fun replaceAll(books: List<BookEntity>) {
        deleteAll()
        insertAll(books)
    }
}
