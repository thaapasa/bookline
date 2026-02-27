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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

class TimelineViewModel(
    private val settingsRepository: SettingsRepository,
    private val bookRepository: BookRepository,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _collapsedSections = MutableStateFlow<Set<String>>(emptySet())

    /** Books from Room, grouped into collapsible timeline sections. */
    val uiState: StateFlow<TimelineUiState> = combine(
        bookRepository.observeTimelineBooks(),
        _collapsedSections,
    ) { books, collapsed ->
        if (settingsRepository.feedUrl.value.isBlank()) {
            TimelineUiState.NoFeedConfigured
        } else {
            TimelineUiState.Success(
                sections = groupIntoSections(books, collapsed),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineUiState.Loading)

    init {
        syncIfNeeded()
    }

    /** Re-check whether a sync is needed (e.g. after returning from settings). */
    fun checkSync() {
        syncIfNeeded()
    }

    /** Trigger a manual refresh (e.g. pull-to-refresh). */
    fun refresh() {
        syncFeed()
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

    private fun groupIntoSections(
        books: List<Book>,
        collapsed: Set<String>,
    ): List<TimelineSection> {
        val sections = mutableListOf<TimelineSection>()

        // Currently reading
        val currentlyReading = books.filter { it.readingStatus == ReadingStatus.CurrentlyReading }
        if (currentlyReading.isNotEmpty()) {
            val key = "currently-reading"
            sections.add(
                TimelineSection.Header(
                    key = key,
                    title = "Currently Reading",
                    level = SectionLevel.Top,
                    isCollapsed = key in collapsed,
                    bookCount = currentlyReading.size,
                ),
            )
            if (key !in collapsed) {
                currentlyReading.forEach { sections.add(TimelineSection.BookItem(it)) }
            }
        }

        // Books with read dates, grouped by year then month
        val withDate = books.filter {
            it.readingStatus != ReadingStatus.CurrentlyReading && it.userReadAt != null
        }
        val byYear = withDate.groupBy { it.userReadAt!!.year }
            .toSortedMap(compareByDescending { it })

        for ((year, yearBooks) in byYear) {
            val yearKey = "year-$year"
            val byMonth = yearBooks.groupBy { it.userReadAt!!.month }
                .toSortedMap(compareByDescending { it.value })
            val monthKeys = byMonth.keys.map { "month-$year-${it.value}" }

            sections.add(
                TimelineSection.Header(
                    key = yearKey,
                    title = year.toString(),
                    level = SectionLevel.Year,
                    isCollapsed = yearKey in collapsed,
                    bookCount = yearBooks.size,
                    childKeys = monthKeys,
                ),
            )
            if (yearKey !in collapsed) {
                for ((month, monthBooks) in byMonth) {
                    val monthKey = "month-$year-${month.value}"
                    val monthName = month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    sections.add(
                        TimelineSection.Header(
                            key = monthKey,
                            title = monthName,
                            level = SectionLevel.Month,
                            isCollapsed = monthKey in collapsed,
                            bookCount = monthBooks.size,
                        ),
                    )
                    if (monthKey !in collapsed) {
                        monthBooks.forEach { sections.add(TimelineSection.BookItem(it)) }
                    }
                }
            }
        }

        // Books without read dates
        val noDate = books.filter {
            it.readingStatus != ReadingStatus.CurrentlyReading && it.userReadAt == null
        }
        if (noDate.isNotEmpty()) {
            val key = "no-date"
            sections.add(
                TimelineSection.Header(
                    key = key,
                    title = "Date Unknown",
                    level = SectionLevel.Top,
                    isCollapsed = key in collapsed,
                    bookCount = noDate.size,
                ),
            )
            if (key !in collapsed) {
                noDate.forEach { sections.add(TimelineSection.BookItem(it)) }
            }
        }

        return sections
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

