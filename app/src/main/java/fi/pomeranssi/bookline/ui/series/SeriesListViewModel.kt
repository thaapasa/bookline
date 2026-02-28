package fi.pomeranssi.bookline.ui.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.data.repository.SettingsRepository
import fi.pomeranssi.bookline.domain.model.Series
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SeriesListViewModel(
    private val settingsRepository: SettingsRepository,
    private val bookRepository: BookRepository,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val filterText = MutableStateFlow("")

    val uiState: StateFlow<SeriesListUiState> = bookRepository.observeAllSeries()
        .map { seriesList ->
            if (settingsRepository.feedUrl.value.isBlank()) {
                SeriesListUiState.NoFeedConfigured
            } else {
                SeriesListUiState.Success(series = seriesList)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SeriesListUiState.Loading)

    /** Series list filtered by the current search text. */
    val filteredSeries: StateFlow<List<Series>> =
        combine(uiState, filterText) { state, filter ->
            val allSeries = (state as? SeriesListUiState.Success)?.series.orEmpty()
            if (filter.isBlank()) allSeries
            else allSeries.filter { it.name.contains(filter, ignoreCase = true) }
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
                bookRepository.sync(feedUrl)
            } catch (_: Exception) {
                // Sync failures are non-fatal when we already have cached data
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

sealed interface SeriesListUiState {
    data object Loading : SeriesListUiState
    data object NoFeedConfigured : SeriesListUiState
    data class Success(val series: List<Series>) : SeriesListUiState
}
