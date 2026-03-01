package fi.pomeranssi.bookline.ui.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.domain.model.Book
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SeriesDetailViewModel(
    private val bookRepository: BookRepository,
    initialSeriesName: String,
) : ViewModel() {

    /** Current series name — updated after a rename. */
    private val currentName = MutableStateFlow(initialSeriesName)

    /** Emitted after a successful rename so the UI can navigate back. */
    private val _renamed = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val renamed: MutableSharedFlow<String> = _renamed

    val uiState: StateFlow<SeriesDetailUiState> = currentName
        .flatMapLatest { name ->
            combine(
                bookRepository.observeSeriesBooks(name),
                bookRepository.observeSeriesAliases(name),
            ) { books, aliases ->
                SeriesDetailUiState.Success(
                    seriesName = name,
                    books = books,
                    aliases = aliases,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SeriesDetailUiState.Loading)

    fun renameSeries(newName: String) {
        val oldName = currentName.value
        if (newName.isBlank() || newName == oldName) return
        viewModelScope.launch {
            bookRepository.renameSeries(oldName, newName)
            currentName.value = newName
            _renamed.tryEmit(newName)
        }
    }
}

sealed interface SeriesDetailUiState {
    data object Loading : SeriesDetailUiState
    data class Success(
        val seriesName: String,
        val books: List<Book>,
        val aliases: List<String> = emptyList(),
    ) : SeriesDetailUiState
}
