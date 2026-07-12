package fi.pomeranssi.bookline.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesInfoDao {
    @Query("SELECT * FROM series_info")
    suspend fun getAll(): List<SeriesInfoEntity>

    @Query("SELECT * FROM series_info WHERE displayName = :name")
    suspend fun getByDisplayName(name: String): SeriesInfoEntity?

    @Query("SELECT * FROM series_info WHERE displayName = :name")
    fun observeByDisplayName(name: String): Flow<SeriesInfoEntity?>

    /** Find the series_info row whose parsedNames contain the given name. */
    @Query("SELECT * FROM series_info WHERE parsedNames LIKE '%|' || :name || '|%' LIMIT 1")
    suspend fun findByParsedName(name: String): SeriesInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SeriesInfoEntity)

    @Query("DELETE FROM series_info WHERE displayName = :displayName")
    suspend fun delete(displayName: String)
}
