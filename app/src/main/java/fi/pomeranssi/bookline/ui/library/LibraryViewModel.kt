package fi.pomeranssi.bookline.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.data.repository.SettingsRepository
import fi.pomeranssi.bookline.domain.model.Book
import fi.pomeranssi.bookline.ui.common.SyncHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(
    private val settingsRepository: SettingsRepository,
    private val bookRepository: BookRepository,
) : ViewModel() {

    private companion object {
        const val TAG = "LibraryVM"
    }

    private val syncHelper = SyncHelper(settingsRepository, bookRepository, TAG)
    val isRefreshing: StateFlow<Boolean> = syncHelper.isRefreshing

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
        syncHelper.checkSync(viewModelScope)
    }

    fun checkSync() {
        syncHelper.checkSync(viewModelScope)
    }

    fun refresh() {
        syncHelper.syncFeed(viewModelScope)
    }
}

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object NoFeedConfigured : LibraryUiState
    data class Success(val books: List<Book>) : LibraryUiState
}
