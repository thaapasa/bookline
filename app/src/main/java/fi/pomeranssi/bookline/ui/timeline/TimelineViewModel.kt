package fi.pomeranssi.bookline.ui.timeline

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.data.repository.SettingsRepository
import fi.pomeranssi.bookline.domain.model.Book
import fi.pomeranssi.bookline.domain.model.ReadingStatus
import fi.pomeranssi.bookline.ui.common.SyncCoordinator
import fi.pomeranssi.bookline.ui.common.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.format.TextStyle
import java.util.Locale

class TimelineViewModel(
    private val settingsRepository: SettingsRepository,
    private val bookRepository: BookRepository,
    private val syncCoordinator: SyncCoordinator,
) : ViewModel() {

    private companion object {
        const val TAG = "TimelineVM"
        const val KEY_CURRENTLY_READING = "currently-reading"
        const val KEY_NO_DATE = "no-date"
        fun yearKey(year: Int) = "year-$year"
        fun monthKey(year: Int, monthValue: Int) = "month-$year-$monthValue"
    }

    val isRefreshing: StateFlow<Boolean> = syncCoordinator.isRefreshing
    val lastSyncResult: StateFlow<SyncResult> = syncCoordinator.lastSyncResult

    fun clearSyncError() = syncCoordinator.clearError()

    private val _collapsedSections = MutableStateFlow<Set<String>>(emptySet())
    val allCollapsed: StateFlow<Boolean> = _collapsedSections
        .combine(bookRepository.observeTimelineBooks()) { collapsed, _ ->
            collapsed.isNotEmpty()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Books from Room, grouped into collapsible timeline sections. */
    val uiState: StateFlow<TimelineUiState> = combine(
        bookRepository.observeTimelineBooks(),
        _collapsedSections,
    ) { books, collapsed ->
        if (settingsRepository.feedUrl.value.isBlank()) {
            Log.d(TAG, "Timeline: no feed configured")
            TimelineUiState.NoFeedConfigured
        } else {
            val sections = groupIntoSections(books, collapsed)
            val bookCount = sections.count { it is TimelineSection.BookItem }
            Log.d(
                TAG,
                "Timeline loaded: $bookCount books in ${sections.size} sections (from ${books.size} total)"
            )
            TimelineUiState.Success(sections = sections)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineUiState.Loading)

    init {
        syncCoordinator.checkSync(viewModelScope)
    }

    /** Re-check whether a sync is needed (e.g. after returning from settings). */
    fun checkSync() {
        syncCoordinator.checkSync(viewModelScope)
    }

    /** Trigger a manual refresh (e.g. pull-to-refresh). */
    fun refresh() {
        syncCoordinator.requestSync(viewModelScope)
    }

    /** Toggle the collapsed state of a section. */
    fun toggleSection(key: String) {
        _collapsedSections.update { current ->
            if (key in current) current - key else current + key
        }
    }

    /**
     * Toggle a year section. When collapsing a year, also collapse all its months.
     * When expanding a year, also expand all its months.
     */
    fun toggleYear(yearKey: String, monthKeys: List<String>) {
        _collapsedSections.update { current ->
            if (yearKey in current) {
                current - yearKey - monthKeys.toSet()
            } else {
                current + yearKey + monthKeys.toSet()
            }
        }
    }

    /** Collapse all sections. */
    fun collapseAll() {
        val state = uiState.value
        if (state is TimelineUiState.Success) {
            val allKeys = state.sections
                .filterIsInstance<TimelineSection.Header>()
                .map { it.key }
                .toSet()
            _collapsedSections.value = allKeys
        }
    }

    /** Expand all sections. */
    fun expandAll() {
        _collapsedSections.value = emptySet()
    }

    private fun groupIntoSections(
        books: List<Book>,
        collapsed: Set<String>,
    ): List<TimelineSection> = buildList {
        addCurrentlyReadingSection(books, collapsed)
        addReadDateSections(books, collapsed)
        addNoDateSection(books, collapsed)
    }

    private fun MutableList<TimelineSection>.addCurrentlyReadingSection(
        books: List<Book>,
        collapsed: Set<String>,
    ) {
        val currentlyReading = books.filter { it.readingStatus == ReadingStatus.CurrentlyReading }
        if (currentlyReading.isEmpty()) return

        add(
            TimelineSection.Header(
                key = KEY_CURRENTLY_READING,
                title = "Currently Reading",
                level = SectionLevel.Top,
                isCollapsed = KEY_CURRENTLY_READING in collapsed,
                bookCount = currentlyReading.size,
            ),
        )
        if (KEY_CURRENTLY_READING !in collapsed) {
            currentlyReading.forEach { add(TimelineSection.BookItem(it)) }
        }
    }

    private fun MutableList<TimelineSection>.addReadDateSections(
        books: List<Book>,
        collapsed: Set<String>,
    ) {
        val withDate = books.filter {
            it.readingStatus != ReadingStatus.CurrentlyReading && it.userReadAt != null
        }
        val byYear = withDate.groupBy { it.userReadAt!!.year }
            .toSortedMap(compareByDescending { it })

        for ((year, yearBooks) in byYear) {
            val yKey = yearKey(year)
            val byMonth = yearBooks.groupBy { it.userReadAt!!.month }
                .toSortedMap(compareByDescending { it.value })
            val monthKeys = byMonth.keys.map { monthKey(year, it.value) }

            add(
                TimelineSection.Header(
                    key = yKey,
                    title = year.toString(),
                    level = SectionLevel.Year,
                    isCollapsed = yKey in collapsed,
                    bookCount = yearBooks.size,
                    childKeys = monthKeys,
                ),
            )
            if (yKey !in collapsed) {
                for ((month, monthBooks) in byMonth) {
                    val mKey = monthKey(year, month.value)
                    val monthName = month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    add(
                        TimelineSection.Header(
                            key = mKey,
                            title = monthName,
                            level = SectionLevel.Month,
                            isCollapsed = mKey in collapsed,
                            bookCount = monthBooks.size,
                        ),
                    )
                    if (mKey !in collapsed) {
                        monthBooks.forEach { add(TimelineSection.BookItem(it)) }
                    }
                }
            }
        }
    }

    private fun MutableList<TimelineSection>.addNoDateSection(
        books: List<Book>,
        collapsed: Set<String>,
    ) {
        val noDate = books.filter {
            it.readingStatus != ReadingStatus.CurrentlyReading && it.userReadAt == null
        }
        if (noDate.isEmpty()) return

        add(
            TimelineSection.Header(
                key = KEY_NO_DATE,
                title = "Date Unknown",
                level = SectionLevel.Top,
                isCollapsed = KEY_NO_DATE in collapsed,
                bookCount = noDate.size,
            ),
        )
        if (KEY_NO_DATE !in collapsed) {
            noDate.forEach { add(TimelineSection.BookItem(it)) }
        }
    }
}

sealed interface TimelineUiState {
    data object Loading : TimelineUiState
    data object NoFeedConfigured : TimelineUiState
    data class Success(val sections: List<TimelineSection>) : TimelineUiState
}

enum class SectionLevel { Top, Year, Month }

sealed interface TimelineSection {
    data class Header(
        val key: String,
        val title: String,
        val level: SectionLevel,
        val isCollapsed: Boolean,
        val bookCount: Int,
        val childKeys: List<String> = emptyList(),
    ) : TimelineSection

    data class BookItem(val book: Book) : TimelineSection
}

