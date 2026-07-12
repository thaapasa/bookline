package fi.pomeranssi.bookline.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.pomeranssi.bookline.ui.theme.BooklineTheme

/**
 * Pull-to-refresh wrapper that uses the platform pull-down behavior
 * and shows a full-width [LinearProgressIndicator] at the top while
 * refreshing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableContent(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = false,
                modifier = Modifier
                    .align(Alignment.TopCenter),
            )
        },
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
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

@Preview(showBackground = true, heightDp = 200)
@Composable
private fun RefreshableContentRefreshingPreview() {
    BooklineTheme(dynamicColor = false) {
        RefreshableContent(
            isRefreshing = true,
            onRefresh = {},
        ) {
            Text(
                text = "List content goes here",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
