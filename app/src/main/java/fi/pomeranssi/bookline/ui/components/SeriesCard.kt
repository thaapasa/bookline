package fi.pomeranssi.bookline.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import fi.pomeranssi.bookline.R
import fi.pomeranssi.bookline.domain.model.Series

/**
 * Card displaying a book series with an overlapping fan of up to 3 covers.
 */
@Composable
fun SeriesCard(
    series: Series,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Fan of overlapping covers
            CoverFan(
                coverUrls = series.coverUrls,
                seriesName = series.name,
                modifier = Modifier.height(120.dp),
            )

            // Series info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
                    .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            ) {
                Text(
                    text = series.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = series.authors.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.weight(1f))

                val bookCount = series.books.size
                Text(
                    text = "$bookCount ${if (bookCount == 1) "book" else "books"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CoverFan(
    coverUrls: List<String>,
    seriesName: String,
    modifier: Modifier = Modifier,
) {
    if (coverUrls.isEmpty()) {
        Box(
            modifier = modifier.width(85.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val coverWidth = 60.dp
    val overlap = 20.dp
    val totalCovers = coverUrls.size.coerceAtMost(3)
    val totalWidth = coverWidth + (overlap * (totalCovers - 1))

    Box(
        modifier = modifier.width(totalWidth + 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        coverUrls.take(3).forEachIndexed { index, url ->
            val coverModifier = Modifier
                .offset(x = (overlap * index) + 4.dp)
                .zIndex((totalCovers - index).toFloat())
                .size(width = coverWidth, height = 90.dp)
                .clip(MaterialTheme.shapes.extraSmall)

            if (LocalInspectionMode.current) {
                Image(
                    painter = painterResource(R.drawable.book_cover_placeholder),
                    contentDescription = "Cover from $seriesName",
                    contentScale = ContentScale.Crop,
                    modifier = coverModifier,
                )
            } else {
                AsyncImage(
                    model = url,
                    contentDescription = "Cover from $seriesName",
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.book_cover_placeholder),
                    modifier = coverModifier,
                )
            }
        }
    }
}
