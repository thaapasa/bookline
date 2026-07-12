package fi.pomeranssi.bookline.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import fi.pomeranssi.bookline.R
import fi.pomeranssi.bookline.domain.model.Book
import fi.pomeranssi.bookline.domain.model.ReadingStatus
import fi.pomeranssi.bookline.ui.common.PreviewData
import fi.pomeranssi.bookline.ui.common.SyncResult
import fi.pomeranssi.bookline.ui.components.BookCard
import fi.pomeranssi.bookline.ui.components.EmptyContent
import fi.pomeranssi.bookline.ui.components.NoFeedConfiguredContent
import fi.pomeranssi.bookline.ui.components.RefreshableContent
import fi.pomeranssi.bookline.ui.components.SearchField
import fi.pomeranssi.bookline.ui.components.SyncErrorBanner
import fi.pomeranssi.bookline.ui.theme.BooklineTheme

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBookClick: (bookId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val lastSyncResult by viewModel.lastSyncResult.collectAsState()

    LifecycleResumeEffect(viewModel) {
        viewModel.checkSync()
        onPauseOrDispose { }
    }

    when (val state = uiState) {
        LibraryUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        LibraryUiState.NoFeedConfigured -> {
            NoFeedConfiguredContent(modifier = modifier.fillMaxSize())
        }

        is LibraryUiState.Success -> {
            val filteredBooks by viewModel.filteredBooks.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            val selectedStatus by viewModel.selectedStatus.collectAsState()
            val selectedShelf by viewModel.selectedShelf.collectAsState()
            val availableShelves by viewModel.availableShelves.collectAsState()

            if (state.books.isEmpty() && !isRefreshing) {
                EmptyContent(
                    message = stringResource(R.string.empty_no_books),
                    modifier = modifier.fillMaxSize(),
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                            contentDescription = null,
                            modifier = Modifier.padding(bottom = 16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    onRefresh = { viewModel.refresh() },
                )
            } else {
                RefreshableContent(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = modifier.fillMaxSize(),
                ) {
                    LibraryContent(
                        books = filteredBooks,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.searchQuery.value = it },
                        selectedStatus = selectedStatus,
                        onStatusSelected = { viewModel.selectedStatus.value = it },
                        selectedShelf = selectedShelf,
                        onShelfSelected = { viewModel.selectedShelf.value = it },
                        availableShelves = availableShelves,
                        unshelvedFilter = viewModel.unshelvedFilter,
                        syncErrorMessage = (lastSyncResult as? SyncResult.Error)?.message,
                        onRetrySync = { viewModel.refresh() },
                        onBookClick = onBookClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryContent(
    books: List<Book>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedStatus: ReadingStatus?,
    onStatusSelected: (ReadingStatus?) -> Unit,
    selectedShelf: String?,
    onShelfSelected: (String?) -> Unit,
    availableShelves: List<String>,
    unshelvedFilter: String,
    syncErrorMessage: String?,
    onRetrySync: () -> Unit,
    onBookClick: (bookId: String) -> Unit,
) {
    Column {
        val hasActiveFilters =
            searchQuery.isNotEmpty() ||
                selectedShelf != null ||
                selectedStatus != null
        SearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = stringResource(R.string.search_books_placeholder),
            hasActiveFilters = hasActiveFilters,
            onClearAll = {
                onSearchQueryChange("")
                onShelfSelected(null)
                onStatusSelected(null)
            },
        )

        var statusExpanded by rememberSaveable { mutableStateOf(false) }

        if (availableShelves.isNotEmpty()) {
            Row {
                LazyRow(
                    contentPadding = PaddingValues(start = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(bottom = 4.dp),
                ) {
                    item {
                        FilterChip(
                            selected = selectedShelf == null,
                            onClick = { onShelfSelected(null) },
                            label = { Text(stringResource(R.string.filter_all)) },
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedShelf == unshelvedFilter,
                            onClick = {
                                onShelfSelected(
                                    if (selectedShelf == unshelvedFilter) {
                                        null
                                    } else {
                                        unshelvedFilter
                                    },
                                )
                            },
                            label = { Text(stringResource(R.string.filter_unshelved)) },
                        )
                    }
                    items(availableShelves) { shelf ->
                        FilterChip(
                            selected = selectedShelf == shelf,
                            onClick = {
                                onShelfSelected(if (selectedShelf == shelf) null else shelf)
                            },
                            label = { Text(shelf) },
                        )
                    }
                }
                if (!statusExpanded) {
                    IconButton(onClick = { statusExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = stringResource(R.string.action_show_status_filter),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = statusExpanded) {
            val statuses =
                listOf(
                    ReadingStatus.Read to stringResource(R.string.filter_read),
                    ReadingStatus.CurrentlyReading to stringResource(R.string.filter_currently_reading),
                    ReadingStatus.ToRead to stringResource(R.string.filter_to_read),
                )
            Row {
                LazyRow(
                    contentPadding = PaddingValues(start = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(bottom = 4.dp),
                ) {
                    item {
                        FilterChip(
                            selected = selectedStatus == null,
                            onClick = { onStatusSelected(null) },
                            label = { Text(stringResource(R.string.filter_all)) },
                        )
                    }
                    items(statuses) { (status, label) ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = {
                                onStatusSelected(if (selectedStatus == status) null else status)
                            },
                            label = { Text(label) },
                        )
                    }
                }
                IconButton(onClick = {
                    statusExpanded = false
                    onStatusSelected(null)
                }) {
                    Icon(
                        imageVector = Icons.Default.ExpandLess,
                        contentDescription = stringResource(R.string.action_hide_status_filter),
                    )
                }
            }
        }

        LazyColumn(
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 8.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (syncErrorMessage != null) {
                item(key = "__sync_error__") {
                    SyncErrorBanner(
                        message = syncErrorMessage,
                        onRetry = onRetrySync,
                    )
                }
            }
            items(
                items = books,
                key = { it.bookId },
            ) { book ->
                BookCard(
                    book = book,
                    onClick = { onBookClick(book.bookId) },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun LibraryContentPreview() {
    BooklineTheme(dynamicColor = false) {
        LibraryContent(
            books = PreviewData.books,
            searchQuery = "",
            onSearchQueryChange = {},
            selectedStatus = null,
            onStatusSelected = {},
            selectedShelf = null,
            onShelfSelected = {},
            availableShelves = listOf("fantasy", "sci-fi"),
            unshelvedFilter = "__unshelved__",
            syncErrorMessage = null,
            onRetrySync = {},
            onBookClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun LibraryContentFilteredWithErrorPreview() {
    BooklineTheme(dynamicColor = false) {
        LibraryContent(
            books = listOf(PreviewData.bookRead),
            searchQuery = "horizon",
            onSearchQueryChange = {},
            selectedStatus = null,
            onStatusSelected = {},
            selectedShelf = "fantasy",
            onShelfSelected = {},
            availableShelves = listOf("fantasy", "sci-fi"),
            unshelvedFilter = "__unshelved__",
            syncErrorMessage = "Could not reach Goodreads: connection timed out",
            onRetrySync = {},
            onBookClick = {},
        )
    }
}
