package fi.pomeranssi.bookline.ui.series

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.data.repository.SettingsRepository
import fi.pomeranssi.bookline.domain.model.Series
import fi.pomeranssi.bookline.ui.common.SyncCoordinator
import fi.pomeranssi.bookline.ui.common.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SeriesListViewModel(
    private val settingsRepository: SettingsRepository,
    private val bookRepository: BookRepository,
    private val syncCoordinator: SyncCoordinator,
) : ViewModel() {

    private companion object {
        const val TAG = "SeriesListVM"
    }

    val isRefreshing: StateFlow<Boolean> = syncCoordinator.isRefreshing
    val lastSyncResult: StateFlow<SyncResult> = syncCoordinator.lastSyncResult

    fun clearSyncError() = syncCoordinator.clearError()

    val filterText = MutableStateFlow("")

    val uiState: StateFlow<SeriesListUiState> = bookRepository.observeAllSeries()
        .map { seriesList ->
            if (settingsRepository.feedUrl.value.isBlank()) {
                Log.d(TAG, "Series: no feed configured")
                SeriesListUiState.NoFeedConfigured
            } else {
                Log.d(TAG, "Series loaded: ${seriesList.size} series")
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
        syncCoordinator.checkSync(viewModelScope)
    }

    fun checkSync() {
        syncCoordinator.checkSync(viewModelScope)
    }

    fun refresh() {
        syncCoordinator.requestSync(viewModelScope)
    }
}

sealed interface SeriesListUiState {
    data object Loading : SeriesListUiState
    data object NoFeedConfigured : SeriesListUiState
    data class Success(val series: List<Series>) : SeriesListUiState
}
