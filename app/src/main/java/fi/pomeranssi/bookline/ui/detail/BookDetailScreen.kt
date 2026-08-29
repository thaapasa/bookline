package fi.pomeranssi.bookline.ui.detail

import android.content.Intent
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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import fi.pomeranssi.bookline.R
import fi.pomeranssi.bookline.domain.model.Book
import fi.pomeranssi.bookline.domain.model.ReadingStatus
import fi.pomeranssi.bookline.ui.common.DateFormatters
import fi.pomeranssi.bookline.ui.common.PreviewData
import fi.pomeranssi.bookline.ui.components.BookCover
import fi.pomeranssi.bookline.ui.components.HtmlText
import fi.pomeranssi.bookline.ui.components.LoadingContent
import fi.pomeranssi.bookline.ui.components.didNotFinishLabel
import fi.pomeranssi.bookline.ui.theme.BooklineTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    viewModel: BookDetailViewModel,
    onNavigateBack: () -> Unit,
    onOpenGoodreads: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSeriesClick: (String) -> Unit = {},
) {
    val book by viewModel.book.collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.book_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
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
                onSeriesClick = onSeriesClick,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun BookDetailContent(
    book: Book,
    onOpenGoodreads: (String) -> Unit,
    onSeriesClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCoverDialog by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        // Cover + title header
        BookDetailHeader(
            book = book,
            onCoverClick = { showCoverDialog = true },
            onSeriesClick = onSeriesClick,
        )

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
                text = stringResource(R.string.description_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            HtmlText(
                html = book.bookDescription,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Goodreads links
        if (!book.goodreadsUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            val context = LocalContext.current
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = { onOpenGoodreads(book.goodreadsUrl) },
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.link_in_app))
                }
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, book.goodreadsUrl.toUri())
                        context.startActivity(intent)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.goodreads))
                }
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
private fun BookDetailHeader(
    book: Book,
    onCoverClick: () -> Unit,
    onSeriesClick: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        BookCover(
            imageUrl = book.bestImageUrl,
            contentDescription = stringResource(R.string.cover_of_book, book.title),
            modifier =
                Modifier
                    .size(width = 120.dp, height = 180.dp)
                    .clickable(onClick = onCoverClick),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleLarge,
            )

            if (book.seriesEntries.isNotEmpty()) {
                book.seriesEntries.forEach { entry ->
                    val posLabel =
                        if (entry.position == entry.position.toLong().toDouble()) {
                            "#${entry.position.toLong()}"
                        } else {
                            "#${entry.position}"
                        }
                    Text(
                        text = "${entry.seriesName} $posLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onSeriesClick(entry.seriesName) },
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
                            text = stringResource(R.string.status_currently_reading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }

                ReadingStatus.Read -> {
                    book.userReadAt?.let { date ->
                        Text(
                            text =
                                stringResource(
                                    R.string.status_read_on,
                                    date.format(DateFormatters.displayDate),
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                ReadingStatus.ToRead -> {
                    Text(
                        text = stringResource(R.string.status_to_read),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                ReadingStatus.DidNotFinish -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = didNotFinishLabel(book),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaSection(book: Book) {
    val items =
        buildList {
            book.isbn?.let { add(stringResource(R.string.meta_isbn) to it) }
            book.numPages?.let { add(stringResource(R.string.meta_pages) to it.toString()) }
            book.bookPublishedYear?.let { add(stringResource(R.string.meta_published) to it.toString()) }
            book.userReadAt?.let { add(stringResource(R.string.meta_last_read) to it.format(DateFormatters.displayDate)) }
            book.userDateAdded?.let { add(stringResource(R.string.meta_date_added) to it.format(DateFormatters.displayDate)) }
            book.userDateCreated?.let { add(stringResource(R.string.meta_date_created) to it.format(DateFormatters.displayDate)) }
            if (book.userShelves.isNotEmpty()) {
                add(stringResource(R.string.meta_shelves) to book.userShelves.joinToString(", "))
            }
        }

    if (items.isNotEmpty() || book.averageRating != null) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Average rating with stars
            book.averageRating?.let { rating ->
                val fullStars =
                    kotlin.math
                        .round(rating)
                        .toInt()
                        .coerceIn(0, 5)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.meta_average_rating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(120.dp),
                    )
                    repeat(fullStars) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    repeat(5 - fullStars) {
                        Icon(
                            imageVector = Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f", rating),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

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
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.9f))
                    .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(R.string.cover_of_book, title),
                contentScale = ContentScale.Fit,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
            )
            IconButton(
                onClick = onDismiss,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun BookDetailContentPreview() {
    BooklineTheme(dynamicColor = false) {
        BookDetailContent(
            book = PreviewData.bookRead,
            onOpenGoodreads = {},
            onSeriesClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 480)
@Composable
private fun BookDetailContentToReadPreview() {
    BooklineTheme(dynamicColor = false) {
        BookDetailContent(
            book = PreviewData.bookToRead,
            onOpenGoodreads = {},
            onSeriesClick = {},
        )
    }
}
