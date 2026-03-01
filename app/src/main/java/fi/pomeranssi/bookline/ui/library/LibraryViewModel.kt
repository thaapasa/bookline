package fi.pomeranssi.bookline.ui.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.data.repository.SettingsRepository
import fi.pomeranssi.bookline.domain.model.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val settingsRepository: SettingsRepository,
    private val bookRepository: BookRepository,
) : ViewModel() {

    private companion object {
        const val TAG = "LibraryVM"
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val searchQuery = MutableStateFlow("")
    val selectedShelf = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LibraryUiState> = bookRepository.observeBooks()
        .map { books ->
            if (settingsRepository.feedUrl.value.isBlank()) {
                LibraryUiState.NoFeedConfigured
            } else {
                LibraryUiState.Success(books = books)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState.Loading)

    val availableShelves: StateFlow<List<String>> = bookRepository.observeBooks()
        .map { books ->
            books.flatMap { it.userShelves }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredBooks: StateFlow<List<Book>> =
        combine(uiState, searchQuery, selectedShelf) { state, query, shelf ->
            val allBooks = (state as? LibraryUiState.Success)?.books.orEmpty()
            allBooks
                .filter { book ->
                    (shelf == null || shelf in book.userShelves) &&
                        (query.isBlank() ||
                            book.title.contains(query, ignoreCase = true) ||
                            book.authorName.contains(query, ignoreCase = true))
                }
                .sortedBy { it.title.lowercase() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        syncIfNeeded()
    }

    fun checkSync() {
        syncIfNeeded()
    }

    fun refresh() {
        syncFeed()
    }

    private fun syncIfNeeded() {
        if (settingsRepository.feedUrl.value.isBlank()) return
        if (bookRepository.isSyncNeeded()) {
            syncFeed()
        }
    }

    private fun syncFeed() {
        val feedUrl = settingsRepository.feedUrl.value
        if (feedUrl.isBlank()) return

        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                val count = bookRepository.sync(feedUrl)
                Log.i(TAG, "syncFeed: completed, $count books synced")
            } catch (e: Exception) {
                Log.e(TAG, "syncFeed: failed", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object NoFeedConfigured : LibraryUiState
    data class Success(val books: List<Book>) : LibraryUiState
}
