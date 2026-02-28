package fi.pomeranssi.bookline.ui.shelves

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.data.repository.SettingsRepository
import fi.pomeranssi.bookline.domain.model.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ToReadViewModel(
    private val settingsRepository: SettingsRepository,
    private val bookRepository: BookRepository,
) : ViewModel() {

    val books: Flow<List<Book>> = bookRepository.observeToReadBooks()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
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
