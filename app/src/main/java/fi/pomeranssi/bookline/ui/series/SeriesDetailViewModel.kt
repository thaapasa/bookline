package fi.pomeranssi.bookline.ui.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.domain.model.Book
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SeriesDetailViewModel(
    private val bookRepository: BookRepository,
    val seriesName: String,
) : ViewModel() {

    val uiState: StateFlow<SeriesDetailUiState> = bookRepository.observeSeriesBooks(seriesName)
        .map { books ->
            SeriesDetailUiState.Success(seriesName = seriesName, books = books)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SeriesDetailUiState.Loading)
}

sealed interface SeriesDetailUiState {
    data object Loading : SeriesDetailUiState
    data class Success(
        val seriesName: String,
        val books: List<Book>,
    ) : SeriesDetailUiState
}
