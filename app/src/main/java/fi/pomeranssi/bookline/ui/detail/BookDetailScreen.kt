package fi.pomeranssi.bookline.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import fi.pomeranssi.bookline.domain.model.Book
import fi.pomeranssi.bookline.domain.model.ReadingStatus
import fi.pomeranssi.bookline.ui.components.HtmlText
import fi.pomeranssi.bookline.ui.components.LoadingContent
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    viewModel: BookDetailViewModel,
    onNavigateBack: () -> Unit,
    onOpenGoodreads: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val book by viewModel.book.collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        val currentBook = book
        if (currentBook == null) {
            LoadingContent(modifier = Modifier.padding(innerPadding))
        } else {
            BookDetailContent(
                book = currentBook,
                onOpenGoodreads = onOpenGoodreads,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun BookDetailContent(
    book: Book,
    onOpenGoodreads: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCoverDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Cover + title header
        Row(modifier = Modifier.fillMaxWidth()) {
            val imageUrl = book.bestImageUrl
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Cover of ${book.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 120.dp, height = 180.dp)
                        .clickable { showCoverDialog = true },
                )
            } else {
                Box(
                    modifier = Modifier.size(width = 120.dp, height = 180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleLarge,
                )

                // Series membership
                if (book.seriesEntries.isNotEmpty()) {
                    book.seriesEntries.forEach { entry ->
                        val posLabel = if (entry.position == entry.position.toLong().toDouble()) {
                            "#${entry.position.toLong()}"
                        } else {
                            "#${entry.position}"
                        }
                        Text(
                            text = "${entry.seriesName} $posLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.authorName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Rating
                if (book.userRating > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(book.userRating) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                // Reading status
                Spacer(modifier = Modifier.height(8.dp))
                when (book.readingStatus) {
                    ReadingStatus.CurrentlyReading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Currently reading",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }

                    ReadingStatus.Read -> {
                        book.userReadAt?.let { date ->
                            Text(
                                text = "Read ${date.format(DATE_FORMATTER)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    ReadingStatus.ToRead -> {
                        Text(
                            text = "To read",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Meta info
        MetaSection(book)

        // Description
        if (!book.bookDescription.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Description",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            HtmlText(
                html = book.bookDescription,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Goodreads link
        if (!book.goodreadsUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { onOpenGoodreads(book.goodreadsUrl) },
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("View on Goodreads")
            }
        }
    }

    // Full-screen cover dialog
    if (showCoverDialog && book.bestImageUrl != null) {
        FullScreenCoverDialog(
            imageUrl = book.bestImageUrl!!,
            title = book.title,
            onDismiss = { showCoverDialog = false },
        )
    }
}

@Composable
private fun MetaSection(book: Book) {
    val items = buildList {
        book.isbn?.let { add("ISBN" to it) }
        book.numPages?.let { add("Pages" to it.toString()) }
        book.bookPublishedYear?.let { add("Published" to it.toString()) }
        book.averageRating?.let { add("Average rating" to String.format("%.2f", it)) }
        book.userReadAt?.let { add("Last read" to it.format(DATE_FORMATTER)) }
        book.userDateAdded?.let { add("Date added" to it.format(DATE_FORMATTER)) }
        book.userDateCreated?.let { add("Date created" to it.format(DATE_FORMATTER)) }
        if (book.userShelves.isNotEmpty()) {
            add("Shelves" to book.userShelves.joinToString(", "))
        }
    }

    if (items.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { (label, value) ->
                Row {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(120.dp),
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun FullScreenCoverDialog(
    imageUrl: String,
    title: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.9f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Cover of $title",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        }
    }
}

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy")
