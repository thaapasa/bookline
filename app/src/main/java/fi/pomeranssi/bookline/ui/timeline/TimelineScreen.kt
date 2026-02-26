package fi.pomeranssi.bookline.ui.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fi.pomeranssi.bookline.domain.model.Book
import fi.pomeranssi.bookline.ui.components.BookCard
import fi.pomeranssi.bookline.ui.components.EmptyContent
import fi.pomeranssi.bookline.ui.components.ErrorContent
import fi.pomeranssi.bookline.ui.components.LoadingContent

@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is TimelineUiState.Initial,
        is TimelineUiState.Loading,
            -> LoadingContent(modifier)

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

        is TimelineUiState.Error -> ErrorContent(
            message = state.message,
            onRetry = viewModel::loadBooks,
            modifier = modifier,
        )

        is TimelineUiState.Success -> {
            if (state.books.isEmpty()) {
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
                BookList(books = state.books, modifier = modifier)
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

