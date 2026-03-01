package fi.pomeranssi.bookline.ui.shelves

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.data.repository.SettingsRepository
import fi.pomeranssi.bookline.domain.model.ToReadBookItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ToReadViewModel(
    private val settingsRepository: SettingsRepository,
    private val bookRepository: BookRepository,
) : ViewModel() {

    val books: Flow<List<ToReadBookItem>> = bookRepository.observeToReadBooks()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _reorderMode = MutableStateFlow(false)
    val reorderMode: StateFlow<Boolean> = _reorderMode.asStateFlow()

    fun toggleReorderMode() {
        _reorderMode.value = !_reorderMode.value
    }

    /**
     * Called when a book has been dragged to a new position.
     * [bookId] is the moved book, [reorderedItems] is the list after the move.
     */
    fun onBookMoved(bookId: String, reorderedItems: List<ToReadBookItem>) {
        val index = reorderedItems.indexOfFirst { it.book.bookId == bookId }
        if (index < 0) return

        val above = if (index > 0) reorderedItems[index - 1].effectiveSortDateMs else null
        val below = if (index < reorderedItems.size - 1) reorderedItems[index + 1].effectiveSortDateMs else null

        val newSortDateMs = calculateNewSortDate(above, below)

        viewModelScope.launch {
            bookRepository.updateToReadSortDate(bookId, newSortDateMs)
        }
    }

    private fun calculateNewSortDate(above: Long?, below: Long?): Long {
        return when {
            // Moved to the top
            above == null && below != null -> {
                val now = System.currentTimeMillis()
                if (below < now) {
                    now
                } else {
                    below + MS_PER_DAY
                }
            }
            // Moved to the bottom
            above != null && below == null -> above - MS_PER_DAY
            // Moved between two books
            above != null && below != null -> (above + below) / 2
            // Only item in list
            else -> System.currentTimeMillis()
        }
    }

    fun refresh() {
        val feedUrl = settingsRepository.feedUrl.value
        if (feedUrl.isBlank()) {
            Log.d(TAG, "refresh: skipped, no feed URL")
            return
        }

        Log.i(TAG, "refresh: starting sync")
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                val count = bookRepository.sync(feedUrl)
                Log.i(TAG, "refresh: completed, $count books synced")
            } catch (e: Exception) {
                Log.e(TAG, "refresh: failed", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private companion object {
        const val TAG = "ToReadVM"
        const val MS_PER_DAY = 86_400_000L
    }
}
