package fi.pomeranssi.bookline.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fi.pomeranssi.bookline.R

/**
 * Reusable book cover image with placeholder and fallback icon.
 *
 * When [imageUrl] is non-null, renders an AsyncImage (or static placeholder
 * in preview/inspection mode). When null, shows a centered Book icon.
 *
 * When [isStale] is true, a small warning badge is shown on the cover.
 */
@Composable
fun BookCover(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    isStale: Boolean = false,
) {
    Box(modifier = modifier) {
        if (imageUrl != null) {
            if (LocalInspectionMode.current) {
                Image(
                    painter = painterResource(R.drawable.book_cover_placeholder),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.book_cover_placeholder),
                    modifier = Modifier.matchParentSize(),
                )
            }
        } else {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (isStale) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Not in latest sync",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(16.dp),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
