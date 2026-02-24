package fi.pomeranssi.bookline.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.data.repository.SettingsRepository
import fi.pomeranssi.bookline.domain.model.Book
import fi.pomeranssi.bookline.domain.model.ReadingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimelineViewModel(
    private val settingsRepository: SettingsRepository,
    private val bookRepository: BookRepository = BookRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<TimelineUiState>(TimelineUiState.Initial)
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    init {
        loadBooks()
    }

    fun loadBooks() {
        val feedUrl = settingsRepository.feedUrl.value
        if (feedUrl.isBlank()) {
            _uiState.value = TimelineUiState.NoFeedConfigured
            return
        }

        _uiState.value = TimelineUiState.Loading

        viewModelScope.launch {
            try {
                val allBooks = bookRepository.getBooks(feedUrl)
                // Timeline shows read + currently-reading books, sorted by read date descending
                val timelineBooks = allBooks
                    .filter { it.readingStatus != ReadingStatus.ToRead }
                    .sortedByDescending { it.userReadAt }
                _uiState.value = TimelineUiState.Success(books = timelineBooks)
            } catch (e: Exception) {
                _uiState.value = TimelineUiState.Error(
                    message = e.message ?: "Failed to load feed",
                )
            }
        }
    }
}

sealed interface TimelineUiState {
    data object Initial : TimelineUiState
    data object Loading : TimelineUiState
    data object NoFeedConfigured : TimelineUiState
    data class Success(val books: List<Book>) : TimelineUiState
    data class Error(val message: String) : TimelineUiState
}

