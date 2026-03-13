package fi.pomeranssi.bookline.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.data.repository.SettingsRepository
import fi.pomeranssi.bookline.domain.model.Book
import fi.pomeranssi.bookline.domain.model.ReadingStatus
import fi.pomeranssi.bookline.ui.common.SyncCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(
    private val settingsRepository: SettingsRepository,
    private val bookRepository: BookRepository,
    private val syncCoordinator: SyncCoordinator,
) : ViewModel() {

    private companion object {
        const val TAG = "LibraryVM"
        val STANDARD_SHELVES = setOf("to-read", "currently-reading", "read")
    }

    /** Sentinel value for filtering to books with no custom shelves. */
    val unshelvedFilter = "__unshelved__"

    val isRefreshing: StateFlow<Boolean> = syncCoordinator.isRefreshing

    val searchQuery = MutableStateFlow("")
    val selectedStatus = MutableStateFlow<ReadingStatus?>(null)
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

    /** Custom (user-defined) shelves present in the library. */
    val availableShelves: StateFlow<List<String>> = bookRepository.observeBooks()
        .map { books ->
            books.flatMap { it.userShelves }
                .filter { it !in STANDARD_SHELVES }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredBooks: StateFlow<List<Book>> =
        combine(uiState, searchQuery, selectedStatus, selectedShelf) { state, query, status, shelf ->
            val allBooks = (state as? LibraryUiState.Success)?.books.orEmpty()
            allBooks
                .filter { book ->
                    val matchesStatus = status == null || book.readingStatus == status
                    val matchesShelf = when (shelf) {
                        null -> true
                        unshelvedFilter ->
                            (book.userShelves.toSet() - STANDARD_SHELVES).isEmpty()
                        else -> shelf in book.userShelves
                    }
                    matchesStatus && matchesShelf &&
                        (query.isBlank() ||
                            book.title.contains(query, ignoreCase = true) ||
                            book.authorName.contains(query, ignoreCase = true))
                }
                .sortedBy { it.title.lowercase() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        syncCoordinator.checkSync(viewModelScope)
    }

    fun checkSync() {
        syncCoordinator.checkSync(viewModelScope)
    }

    fun refresh() {
        syncCoordinator.requestSync(viewModelScope)
    }
}

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object NoFeedConfigured : LibraryUiState
    data class Success(val books: List<Book>) : LibraryUiState
}
