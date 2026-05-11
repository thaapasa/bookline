package fi.pomeranssi.bookline

import android.app.Application
import android.content.Context
import fi.pomeranssi.bookline.data.db.BooklineDatabase
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.data.repository.SettingsRepository
import fi.pomeranssi.bookline.ui.common.SyncCoordinator

class BooklineApplication : Application() {

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(applicationContext)
    }

    val database: BooklineDatabase by lazy {
        BooklineDatabase.getInstance(applicationContext)
    }

    val bookRepository: BookRepository by lazy {
        BookRepository(
            database.bookDao(),
            database.bookSeriesDao(),
            database.seriesInfoDao(),
            settingsRepository,
            database.bookSortOverrideDao(),
        )
    }

    val syncCoordinator: SyncCoordinator by lazy {
        SyncCoordinator(settingsRepository, bookRepository)
    }
}

val Context.booklineApp: BooklineApplication
    get() = applicationContext as BooklineApplication
