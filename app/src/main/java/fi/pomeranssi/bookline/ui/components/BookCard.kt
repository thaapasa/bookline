package fi.pomeranssi.bookline.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.pomeranssi.bookline.R
import fi.pomeranssi.bookline.domain.model.Book
import fi.pomeranssi.bookline.domain.model.ReadingStatus
import fi.pomeranssi.bookline.ui.common.DateFormatters
import fi.pomeranssi.bookline.ui.common.PreviewData
import fi.pomeranssi.bookline.ui.theme.BooklineTheme

private val DESATURATED = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
fun BookCard(
    book: Book,
    modifier: Modifier = Modifier,
    showSeriesInfo: Boolean = true,
    onClick: () -> Unit = {},
) {
    // Did-not-finish books are toned down: flatter card, grey cover, muted accents
    val didNotFinish = book.readingStatus == ReadingStatus.DidNotFinish
    val accentColor =
        if (didNotFinish) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            onClick = onClick,
            colors =
                if (didNotFinish) {
                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                } else {
                    CardDefaults.cardColors()
                },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(if (book.isStale) Modifier.alpha(0.5f) else Modifier),
            ) {
                // Cover image — flush with the card edge
                BookCover(
                    imageUrl = book.bestImageUrl,
                    contentDescription = stringResource(R.string.cover_of_book, book.title),
                    colorFilter = if (didNotFinish) DESATURATED else null,
                    modifier = Modifier.size(width = 85.dp, height = 120.dp),
                )

                // Book info
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(120.dp)
                            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
                ) {
                    // Title with optional series suffix
                    val firstSeries = book.seriesEntries.firstOrNull()
                    val titleText =
                        if (showSeriesInfo && firstSeries != null) {
                            val posLabel =
                                if (firstSeries.position == firstSeries.position.toLong().toDouble()) {
                                    "#${firstSeries.position.toLong()}"
                                } else {
                                    "#${firstSeries.position}"
                                }
                            val seriesColor = accentColor
                            val titleStyle = MaterialTheme.typography.titleSmall
                            buildAnnotatedString {
                                append(book.title)
                                append(" ")
                                withStyle(titleStyle.toSpanStyle().copy(color = seriesColor)) {
                                    append("(${firstSeries.seriesName} $posLabel)")
                                }
                            }
                        } else {
                            buildAnnotatedString { append(book.title) }
                        }
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = book.authorName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Rating row
                    if (book.userRating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(book.userRating) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = accentColor,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Status / date row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        when (book.readingStatus) {
                            ReadingStatus.CurrentlyReading -> {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.tertiary,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.status_currently_reading),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }

                            ReadingStatus.Read -> {
                                book.userReadAt?.let { date ->
                                    Text(
                                        text =
                                            stringResource(
                                                R.string.status_read_on,
                                                date.format(DateFormatters.displayDate),
                                            ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            ReadingStatus.ToRead -> {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.status_to_read),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }

                            ReadingStatus.DidNotFinish -> {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = didNotFinishLabel(book),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    // Page count + published year
                    val meta =
                        buildList {
                            book.numPages?.let { add(pluralStringResource(R.plurals.page_count, it, it)) }
                            book.bookPublishedYear?.let { add(stringResource(R.string.published_in_year, it)) }
                        }.joinToString(" · ")
                    if (meta.isNotEmpty()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
            // Close Card
        }
        // Stale badge — overlaid on top-right of the card at full opacity
        if (book.isStale) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp, top = 8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = CircleShape,
                        ).padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = stringResource(R.string.badge_not_in_latest_sync),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** "Did not finish" label, with the date the book was set aside when Goodreads has one. */
@Composable
internal fun didNotFinishLabel(book: Book): String =
    book.userReadAt?.let { date ->
        stringResource(R.string.status_did_not_finish_on, date.format(DateFormatters.displayDate))
    } ?: stringResource(R.string.status_did_not_finish)

@Preview(showBackground = true)
@Composable
private fun BookCardPreview() {
    BooklineTheme(dynamicColor = false) {
        BookCard(book = PreviewData.bookRead, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun BookCardCurrentlyReadingPreview() {
    BooklineTheme(dynamicColor = false) {
        BookCard(
            book = PreviewData.bookCurrentlyReading,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BookCardDidNotFinishPreview() {
    BooklineTheme(dynamicColor = false) {
        BookCard(
            book = PreviewData.bookDidNotFinish,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BookCardDidNotFinishNoDatePreview() {
    BooklineTheme(dynamicColor = false) {
        BookCard(
            book = PreviewData.bookDidNotFinish.copy(userReadAt = null),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BookCardStalePreview() {
    BooklineTheme(dynamicColor = false) {
        BookCard(
            book = PreviewData.bookRead.copy(isStale = true),
            modifier = Modifier.padding(16.dp),
        )
    }
}
