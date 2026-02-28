package fi.pomeranssi.bookline.ui.shelves

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fi.pomeranssi.bookline.ui.components.BookCard
import fi.pomeranssi.bookline.ui.components.EmptyContent
import fi.pomeranssi.bookline.ui.components.RefreshableContent

@Composable
fun ToReadScreen(
    viewModel: ToReadViewModel,
    onBookClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val books by viewModel.books.collectAsState(initial = emptyList())
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    if (books.isEmpty() && !isRefreshing) {
        EmptyContent(
            message = "Your reading list will appear here.",
            modifier = modifier,
            icon = {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    modifier = Modifier.padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
        )
    } else {
        RefreshableContent(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = modifier,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(items = books, key = { it.bookId }) { book ->
                    BookCard(book = book, onClick = { onBookClick(book.bookId) })
                }
                item { Spacer(modifier = Modifier.height(4.dp)) }
            }
        }
    }
}

