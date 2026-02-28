package fi.pomeranssi.bookline.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BookEntity::class, BookSeriesEntity::class, SeriesInfoEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class BooklineDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun bookSeriesDao(): BookSeriesDao
    abstract fun seriesInfoDao(): SeriesInfoDao

    companion object {
        @Volatile
        private var instance: BooklineDatabase? = null

        fun getInstance(context: Context): BooklineDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BooklineDatabase::class.java,
                    "bookline.db",
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { instance = it }
            }
    }
}
