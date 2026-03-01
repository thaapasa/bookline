package fi.pomeranssi.bookline.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import fi.pomeranssi.bookline.ui.components.BookCard
import fi.pomeranssi.bookline.ui.components.EmptyContent
import fi.pomeranssi.bookline.ui.components.RefreshableContent

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBookClick: (bookId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.currentState.let { state ->
            if (state.isAtLeast(Lifecycle.State.RESUMED)) {
                viewModel.checkSync()
            }
        }
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
            EmptyContent(
                message = "Set up your Goodreads RSS feed in Settings to see your library.",
                modifier = modifier.fillMaxSize(),
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        }

        is LibraryUiState.Success -> {
            val filteredBooks by viewModel.filteredBooks.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
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
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            placeholder = { Text("Search books…") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )

                        if (availableShelves.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp),
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedShelf == null,
                                        onClick = { viewModel.selectedShelf.value = null },
                                        label = { Text("All") },
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
