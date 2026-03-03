package fi.pomeranssi.bookline.ui.goodreads

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val TAG = "GoodreadsScreen"
private const val GOODREADS_URL = "https://www.goodreads.com"
private const val TOGGLE_MODE_URL = "$GOODREADS_URL/toggle_mobile"
private const val REVIEW_LIST_URL = "$GOODREADS_URL/review/list"

/** JavaScript that searches the DOM for the Goodreads RSS feed link. */
private val RSS_DETECTION_JS = """
    (function() {
        var link = document.querySelector('a[href*="/review/list_rss/"]');
        if (link) return link.href;
        var metaLink = document.querySelector('link[type="application/rss+xml"]');
        if (metaLink) return metaLink.href;
        return '';
    })();
""".trimIndent()

/**
 * RSS detection state machine.
 *
 * Flow: Searching(1) → [found] → Toggling(RestoreMode(url)) → Found
 *       Searching(1) → [miss]  → Toggling(RetrySearch)       → Searching(2)
 *       Searching(2) → [any]   → Toggling(RestoreMode(url?)) → Found / NotFound
 */
private sealed interface RssDetectionState {
    data object Idle : RssDetectionState

    /** Loading /review/list; [attempt] is 1 (current mode) or 2 (after toggle). */
    data class Searching(val attempt: Int) : RssDetectionState

    /** Loading toggle URL; [next] decides what happens after. */
    data class Toggling(val next: AfterToggle) : RssDetectionState
    data class Found(val url: String) : RssDetectionState
    data object NotFound : RssDetectionState
}

private sealed interface AfterToggle {
    /** Toggle done → try /review/list again (attempt 2). */
    data object RetrySearch : AfterToggle

    /** Toggle done → show result dialog. */
    data class RestoreMode(val foundUrl: String?) : AfterToggle
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoodreadsScreen(
    modifier: Modifier = Modifier,
    initialUrl: String = GOODREADS_URL,
    onRssFeedDetected: ((String) -> Unit)? = null,
    autoDetect: Boolean = false,
) {
    var isLoading by remember { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var rssState by remember { mutableStateOf<RssDetectionState>(RssDetectionState.Idle) }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    this.webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            when (val state = rssState) {
                                is RssDetectionState.Searching -> {
                                    if (view == null) return
                                    Log.d(TAG, "Attempt ${state.attempt}: injecting JS on $url")
                                    view.evaluateJavascript(RSS_DETECTION_JS) { result ->
                                        val foundUrl = result
                                            ?.trim('"')
                                            ?.takeIf { it.isNotEmpty() && it != "null" }
                                        if (foundUrl != null) {
                                            // Found — toggle back to restore original mode
                                            Log.d(
                                                TAG,
                                                "RSS found: $foundUrl, toggling to restore mode"
                                            )
                                            rssState = RssDetectionState.Toggling(
                                                AfterToggle.RestoreMode(foundUrl),
                                            )
                                            view.loadUrl(TOGGLE_MODE_URL)
                                        } else if (state.attempt == 1) {
                                            // First attempt missed — toggle and retry
                                            Log.d(TAG, "Not found on attempt 1, toggling mode")
                                            rssState = RssDetectionState.Toggling(
                                                AfterToggle.RetrySearch,
                                            )
                                            view.loadUrl(TOGGLE_MODE_URL)
                                        } else {
                                            // Second attempt also missed — toggle to restore, show NotFound
                                            Log.d(
                                                TAG,
                                                "Not found on attempt 2, toggling to restore mode"
                                            )
                                            rssState = RssDetectionState.Toggling(
                                                AfterToggle.RestoreMode(null),
                                            )
                                            view.loadUrl(TOGGLE_MODE_URL)
                                        }
                                    }
                                }

                                is RssDetectionState.Toggling -> {
                                    when (val next = state.next) {
                                        is AfterToggle.RetrySearch -> {
                                            Log.d(TAG, "Toggled, retrying search")
                                            rssState = RssDetectionState.Searching(attempt = 2)
                                            view?.loadUrl(REVIEW_LIST_URL)
                                        }

                                        is AfterToggle.RestoreMode -> {
                                            Log.d(TAG, "Mode restored, showing result")
                                            rssState = if (next.foundUrl != null) {
                                                RssDetectionState.Found(next.foundUrl)
                                            } else {
                                                RssDetectionState.NotFound
                                            }
                                        }
                                    }
                                }

                                else -> {}
                            }
                        }
                    }

                    if (autoDetect) {
                        rssState = RssDetectionState.Searching(attempt = 1)
                        loadUrl(REVIEW_LIST_URL)
                    } else {
                        loadUrl(initialUrl)
                    }
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Search-in-progress overlay
        val isDetecting = rssState is RssDetectionState.Searching
            || rssState is RssDetectionState.Toggling
        if (isDetecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.inversePrimary,
                    )
                    Text(
                        text = "Searching for RSS feed…",
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        // FAB to trigger RSS feed detection (only in manual mode, not auto-detect)
        if (onRssFeedDetected != null && !autoDetect && rssState is RssDetectionState.Idle) {
            FloatingActionButton(
                onClick = {
                    webView?.let { wv ->
                        rssState = RssDetectionState.Searching(attempt = 1)
                        wv.loadUrl(REVIEW_LIST_URL)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp,
                    focusedElevation = 4.dp,
                    hoveredElevation = 4.dp,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.RssFeed,
                    contentDescription = "Find RSS feed",
                )
            }
        }
    }

    // Result dialogs
    when (val state = rssState) {
        is RssDetectionState.Found -> {
            AlertDialog(
                onDismissRequest = { rssState = RssDetectionState.Idle },
                title = { Text("RSS Feed Found") },
                text = {
                    Text("Found your Goodreads RSS feed URL. Save it to start syncing your books?")
                },
                confirmButton = {
                    TextButton(onClick = {
                        onRssFeedDetected?.invoke(state.url)
                        rssState = RssDetectionState.Idle
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { rssState = RssDetectionState.Idle }) {
                        Text("Cancel")
                    }
                },
            )
        }

        is RssDetectionState.NotFound -> {
            AlertDialog(
                onDismissRequest = { rssState = RssDetectionState.Idle },
                title = { Text("RSS Feed Not Found") },
                text = {
                    Text(
                        "Could not find an RSS feed link on this page. " +
                                "Make sure you are logged in to Goodreads, then try again."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        webView?.let { wv ->
                            rssState = RssDetectionState.Searching(attempt = 1)
                            wv.loadUrl(REVIEW_LIST_URL)
                        }
                    }) {
                        Text("Try Again")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { rssState = RssDetectionState.Idle }) {
                        Text("Dismiss")
                    }
                },
            )
        }

        else -> {}
    }
}

