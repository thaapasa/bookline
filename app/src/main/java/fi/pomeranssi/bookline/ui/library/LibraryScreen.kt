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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import fi.pomeranssi.bookline.domain.model.ReadingStatus
import fi.pomeranssi.bookline.ui.components.BookCard
import fi.pomeranssi.bookline.ui.components.EmptyContent
import fi.pomeranssi.bookline.ui.components.NoFeedConfiguredContent
import fi.pomeranssi.bookline.ui.components.RefreshableContent
import fi.pomeranssi.bookline.ui.components.SearchField

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBookClick: (bookId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

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
                    message = "No books found in your feed.",
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
                    Column {
                        val hasActiveFilters = searchQuery.isNotEmpty()
                            || selectedShelf != null
                            || selectedStatus != null
                        SearchField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            placeholder = "Search books…",
                            hasActiveFilters = hasActiveFilters,
                            onClearAll = {
                                viewModel.searchQuery.value = ""
                                viewModel.selectedShelf.value = null
                                viewModel.selectedStatus.value = null
                            },
                        )

                        var statusExpanded by rememberSaveable { mutableStateOf(false) }

                        if (availableShelves.isNotEmpty()) {
                            Row {
                                LazyRow(
                                    contentPadding = PaddingValues(start = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(bottom = 4.dp),
                                ) {
                                    item {
                                        FilterChip(
                                            selected = selectedShelf == null,
                                            onClick = { viewModel.selectedShelf.value = null },
                                            label = { Text("all") },
                                        )
                                    }
                                    item {
                                        FilterChip(
                                            selected = selectedShelf == viewModel.unshelvedFilter,
                                            onClick = {
                                                viewModel.selectedShelf.value =
                                                    if (selectedShelf == viewModel.unshelvedFilter) null
                                                    else viewModel.unshelvedFilter
                                            },
                                            label = { Text("unshelved") },
                                        )
                                    }
                                    items(availableShelves) { shelf ->
                                        FilterChip(
                                            selected = selectedShelf == shelf,
                                            onClick = {
                                                viewModel.selectedShelf.value =
                                                    if (selectedShelf == shelf) null else shelf
                                            },
                                            label = { Text(shelf) },
                                        )
                                    }
                                }
                                if (!statusExpanded) {
                                    IconButton(onClick = { statusExpanded = true }) {
                                        Icon(
                                            imageVector = Icons.Default.ExpandMore,
                                            contentDescription = "Show reading status filter",
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = statusExpanded) {
                            val statuses = listOf(
                                ReadingStatus.Read to "read",
                                ReadingStatus.CurrentlyReading to "currently reading",
                                ReadingStatus.ToRead to "to read",
                            )
                            Row {
                                LazyRow(
                                    contentPadding = PaddingValues(start = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(bottom = 4.dp),
                                ) {
                                    item {
                                        FilterChip(
                                            selected = selectedStatus == null,
                                            onClick = { viewModel.selectedStatus.value = null },
                                            label = { Text("all") },
                                        )
                                    }
                                    items(statuses) { (status, label) ->
                                        FilterChip(
                                            selected = selectedStatus == status,
                                            onClick = {
                                                viewModel.selectedStatus.value =
                                                    if (selectedStatus == status) null else status
                                            },
                                            label = { Text(label) },
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    statusExpanded = false
                                    viewModel.selectedStatus.value = null
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ExpandLess,
                                        contentDescription = "Hide reading status filter",
                                    )
                                }
                            }
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 8.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = filteredBooks,
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
            }
        }
    }
}
