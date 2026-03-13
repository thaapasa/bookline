package fi.pomeranssi.bookline.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.pomeranssi.bookline.data.db.BooklineDatabase
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val database: BooklineDatabase,
    private val bookRepository: BookRepository,
) : ViewModel() {

    /** Current text in the URL field (may not yet be saved). */
    private val _urlField = MutableStateFlow(settingsRepository.feedUrl.value)
    val urlField: StateFlow<String> = _urlField.asStateFlow()

    /** Whether the last save was successful (for a brief confirmation). */
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    /** Whether the database was just cleared (for a brief confirmation). */
    private val _dbCleared = MutableStateFlow(false)
    val dbCleared: StateFlow<Boolean> = _dbCleared.asStateFlow()

    /** Whether stale books were just flushed (for a brief confirmation). */
    private val _staleFlushed = MutableStateFlow(false)
    val staleFlushed: StateFlow<Boolean> = _staleFlushed.asStateFlow()

    /** Reactive count of books not present in the latest successful sync. */
    val staleBookCount: Flow<Int> = bookRepository.observeStaleBookCount()

    fun syncUrlFromRepository() {
        _urlField.value = settingsRepository.feedUrl.value
        _saved.value = false
        _dbCleared.value = false
        _staleFlushed.value = false
    }

    fun onUrlChanged(value: String) {
        _urlField.value = value
        _saved.value = false
    }

    fun save() {
        settingsRepository.saveFeedUrl(_urlField.value)
        _saved.value = true
    }

    /** Clears books, book-series links, and sort overrides but keeps series name mappings. */
    fun clearDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.w(TAG, "Clearing book data from database (keeping series info)")
            database.bookSortOverrideDao().deleteAll()
            database.bookSeriesDao().deleteAll()
            database.bookDao().deleteAll()
            settingsRepository.lastSyncEpochMs = 0L
            _dbCleared.value = true
            Log.i(TAG, "Book data cleared successfully")
        }
    }

    /** Immediately removes books not present in the latest successful sync. */
    fun flushStaleBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Flushing stale books")
            bookRepository.flushStaleBooks()
            _staleFlushed.value = true
            Log.i(TAG, "Stale books flushed successfully")
        }
    }

    companion object {
        private const val TAG = "SettingsVM"
    }
}

