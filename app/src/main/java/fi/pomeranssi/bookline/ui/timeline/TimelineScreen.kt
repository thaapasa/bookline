package fi.pomeranssi.bookline.ui.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fi.pomeranssi.bookline.domain.model.Book
import fi.pomeranssi.bookline.ui.components.BookCard
import fi.pomeranssi.bookline.ui.components.EmptyContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    when (val state = uiState) {
        is TimelineUiState.NoFeedConfigured -> EmptyContent(
            message = "Set up your Goodreads RSS feed in Settings to see your timeline.",
            modifier = modifier,
            icon = {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    modifier = Modifier.padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
        )

        is TimelineUiState.Loading,
        is TimelineUiState.Success,
            -> {
            val books = (state as? TimelineUiState.Success)?.books.orEmpty()
            if (books.isEmpty() && !isRefreshing) {
                EmptyContent(
                    message = "No books found in your feed.",
                    modifier = modifier,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            modifier = Modifier.padding(bottom = 16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
            } else {
                val pullToRefreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                    state = pullToRefreshState,
                    indicator = {},
                    modifier = modifier,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        BookList(books = books)

                        if (isRefreshing) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookList(
    books: List<Book>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }
        items(items = books, key = { it.bookId }) { book ->
            BookCard(book = book)
        }
        item { Spacer(modifier = Modifier.height(4.dp)) }
    }
}

