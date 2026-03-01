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
        val movedIndex = reorderedItems.indexOfFirst { it.book.bookId == bookId }
        if (movedIndex < 0) return

        val aboveMs = if (movedIndex > 0) reorderedItems[movedIndex - 1].effectiveSortDateMs else null
        val belowMs = if (movedIndex < reorderedItems.size - 1) reorderedItems[movedIndex + 1].effectiveSortDateMs else null

        // If neighbors have same or very close stamps, spread out the whole group
        if (aboveMs != null && belowMs != null && aboveMs - belowMs < SPREAD_INTERVAL) {
            spreadGroup(movedIndex, reorderedItems)
            return
        }

        val newSortDateMs = calculateNewSortDate(aboveMs, belowMs)
        viewModelScope.launch {
            bookRepository.updateToReadSortDate(bookId, newSortDateMs)
        }
    }

    /**
     * When the moved book lands between items with the same (or very close) stamps,
     * find all items in that cluster and assign them evenly-spaced stamps that
     * preserve the current list order.
     *
     * Walks outward from the moved item's *neighbors* (not the moved item itself,
     * whose stamp is stale) to find the full cluster, then spaces all items evenly
     * between the stamps of the adjacent non-group items.
     */
    private fun spreadGroup(movedIndex: Int, items: List<ToReadBookItem>) {
        // Walk outward from the neighbors (which have valid stamps)
        var start = movedIndex - 1
        while (start > 0 &&
            items[start - 1].effectiveSortDateMs - items[start].effectiveSortDateMs < SPREAD_INTERVAL
        ) {
            start--
        }
        var end = movedIndex + 1
        while (end < items.size - 1 &&
            items[end].effectiveSortDateMs - items[end + 1].effectiveSortDateMs < SPREAD_INTERVAL
        ) {
            end++
        }
        // Group is items[start..end], which includes movedIndex between the two walks

        val groupSize = end - start + 1

        // Boundary stamps from adjacent non-group items
        val upperBound = if (start > 0) items[start - 1].effectiveSortDateMs else null
        val lowerBound = if (end < items.size - 1) items[end + 1].effectiveSortDateMs else null

        val topStamp: Long
        val interval: Long

        when {
            upperBound != null && lowerBound != null -> {
                // Spread evenly between the two boundary stamps
                interval = (upperBound - lowerBound) / (groupSize + 1)
                topStamp = upperBound - interval
            }
            upperBound != null -> {
                interval = SPREAD_INTERVAL
                topStamp = upperBound - interval
            }
            lowerBound != null -> {
                interval = SPREAD_INTERVAL
                topStamp = lowerBound + groupSize * interval
            }
            else -> {
                interval = SPREAD_INTERVAL
                topStamp = System.currentTimeMillis()
            }
        }

        val updates = mutableMapOf<String, Long>()
        for (i in start..end) {
            updates[items[i].book.bookId] = topStamp - (i - start) * interval
        }

        Log.d(TAG, "spreadGroup: spreading $groupSize items, interval=${interval}ms")
        viewModelScope.launch {
            bookRepository.updateToReadSortDates(updates)
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
        const val SPREAD_INTERVAL = 600_000L // 10 minutes — used to space out same-stamped items
    }
}
