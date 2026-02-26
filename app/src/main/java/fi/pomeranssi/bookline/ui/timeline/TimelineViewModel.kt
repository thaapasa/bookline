package fi.pomeranssi.bookline.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.data.repository.SettingsRepository
import fi.pomeranssi.bookline.domain.model.Book
import fi.pomeranssi.bookline.domain.model.ReadingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimelineViewModel(
    private val settingsRepository: SettingsRepository,
    private val bookRepository: BookRepository,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** Books from Room, filtered and sorted for the timeline. */
    val uiState: StateFlow<TimelineUiState> = bookRepository.observeBooks()
        .map { allBooks ->
            if (settingsRepository.feedUrl.value.isBlank()) {
                TimelineUiState.NoFeedConfigured
            } else {
                val timelineBooks = allBooks
                    .filter { it.readingStatus != ReadingStatus.ToRead }
                    .sortedByDescending { it.userReadAt }
                TimelineUiState.Success(books = timelineBooks)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineUiState.Loading)

    init {
        syncIfNeeded()
    }

    /** Trigger a manual refresh (e.g. pull-to-refresh). */
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
                bookRepository.sync(feedUrl)
            } catch (_: Exception) {
                // Sync failures are non-fatal when we already have cached data
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

sealed interface TimelineUiState {
    data object Loading : TimelineUiState
    data object NoFeedConfigured : TimelineUiState
    data class Success(val books: List<Book>) : TimelineUiState
}

