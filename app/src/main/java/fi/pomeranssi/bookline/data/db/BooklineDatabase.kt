package fi.pomeranssi.bookline.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BookEntity::class, BookSeriesEntity::class, SeriesInfoEntity::class, BookSortOverrideEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class BooklineDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun bookSeriesDao(): BookSeriesDao
    abstract fun seriesInfoDao(): SeriesInfoDao
    abstract fun bookSortOverrideDao(): BookSortOverrideDao

    companion object {
        @Volatile
        private var instance: BooklineDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS book_sort_overrides (
                        bookId TEXT NOT NULL PRIMARY KEY,
                        sortDateMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): BooklineDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BooklineDatabase::class.java,
                    "bookline.db",
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { instance = it }
            }
    }
}
