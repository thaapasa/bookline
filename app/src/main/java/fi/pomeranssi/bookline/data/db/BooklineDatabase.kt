package fi.pomeranssi.bookline.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BookEntity::class], version = 1, exportSchema = false)
abstract class BooklineDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var instance: BooklineDatabase? = null

        fun getInstance(context: Context): BooklineDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BooklineDatabase::class.java,
                    "bookline.db",
                ).build().also { instance = it }
            }
    }
}
