package fi.pomeranssi.bookline.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fi.pomeranssi.bookline.R
import fi.pomeranssi.bookline.ui.theme.BooklineTheme

/**
 * URL scheme for preview-only cover images. In inspection mode a URL like
 * `preview://preview_cover_x` resolves to the drawable of that name from the
 * debug source set (`src/debug/res/drawable-nodpi/`); the lookup is by name so
 * release builds don't reference debug resources.
 */
internal const val PREVIEW_IMAGE_SCHEME = "preview://"

/**
 * Reusable book cover image with placeholder and fallback icon.
 *
 * When [imageUrl] is non-null, renders an AsyncImage (or static placeholder
 * in preview/inspection mode). When null, shows a centered Book icon.
 */
@Composable
fun BookCover(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    if (imageUrl != null) {
        if (LocalInspectionMode.current) {
            val packageName = LocalContext.current.packageName
            val resources = LocalResources.current
            val previewResId =
                if (imageUrl.startsWith(PREVIEW_IMAGE_SCHEME)) {
                    @Suppress("DiscouragedApi")
                    resources.getIdentifier(
                        imageUrl.removePrefix(PREVIEW_IMAGE_SCHEME),
                        "drawable",
                        packageName,
                    )
                } else {
                    0
                }
            Image(
                painter =
                    painterResource(
                        if (previewResId != 0) previewResId else R.drawable.book_cover_placeholder,
                    ),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier,
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.book_cover_placeholder),
                modifier = modifier,
            )
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookCoverPreview() {
    BooklineTheme(dynamicColor = false) {
        BookCover(
            imageUrl = "preview://preview_cover_beyond_horizon",
            contentDescription = "Cover of a book",
            modifier = Modifier.size(width = 85.dp, height = 120.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BookCoverMissingImagePreview() {
    BooklineTheme(dynamicColor = false) {
        BookCover(
            imageUrl = null,
            contentDescription = "Cover of a book",
            modifier = Modifier.size(width = 85.dp, height = 120.dp),
        )
    }
}
