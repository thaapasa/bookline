package fi.pomeranssi.bookline.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.pomeranssi.bookline.R
import fi.pomeranssi.bookline.ui.theme.BooklineTheme

@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyContent(
    message: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_no_books),
            contentDescription = null,
            contentScale = ContentScale.None,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        if (icon != null) {
            icon()
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        if (onRefresh != null) {
            TextButton(
                onClick = onRefresh,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(stringResource(R.string.action_refresh))
            }
        }
    }
}

/** Shared empty state shown when no Goodreads RSS feed URL has been configured. */
@Composable
fun NoFeedConfiguredContent(modifier: Modifier = Modifier) {
    EmptyContent(
        message = stringResource(R.string.no_feed_configured),
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Default.RssFeed,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
    )
}

@Composable
fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.error_something_went_wrong),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp),
        )
        TextButton(
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(stringResource(R.string.action_retry))
        }
    }
}

@Preview(showBackground = true, heightDp = 240)
@Composable
private fun LoadingContentPreview() {
    BooklineTheme(dynamicColor = false) {
        LoadingContent()
    }
}

@Preview(showBackground = true, heightDp = 400)
@Composable
private fun EmptyContentPreview() {
    BooklineTheme(dynamicColor = false) {
        EmptyContent(
            message = "No books found. Pull down to refresh your library.",
            onRefresh = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 400)
@Composable
private fun NoFeedConfiguredContentPreview() {
    BooklineTheme(dynamicColor = false) {
        NoFeedConfiguredContent()
    }
}

@Preview(showBackground = true, heightDp = 400)
@Composable
private fun ErrorContentPreview() {
    BooklineTheme(dynamicColor = false) {
        ErrorContent(
            message = "Could not reach Goodreads: connection timed out",
            onRetry = {},
        )
    }
}
