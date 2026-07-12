package fi.pomeranssi.bookline.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import fi.pomeranssi.bookline.ui.theme.BooklineTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onFindRssFeed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val urlField by viewModel.urlField.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val dbCleared by viewModel.dbCleared.collectAsState()
    val staleFlushed by viewModel.staleFlushed.collectAsState()
    val staleCount by viewModel.staleBookCount.collectAsState(initial = 0)

    LifecycleResumeEffect(Unit) {
        viewModel.syncUrlFromRepository()
        onPauseOrDispose {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        SettingsContent(
            urlField = urlField,
            onUrlChanged = viewModel::onUrlChanged,
            onSave = viewModel::save,
            saved = saved,
            staleCount = staleCount,
            onFlushStaleBooks = viewModel::flushStaleBooks,
            staleFlushed = staleFlushed,
            onClearDatabase = viewModel::clearDatabase,
            dbCleared = dbCleared,
            onFindRssFeed = onFindRssFeed,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun SettingsContent(
    urlField: String,
    onUrlChanged: (String) -> Unit,
    onSave: () -> Unit,
    saved: Boolean,
    staleCount: Int,
    onFlushStaleBooks: () -> Unit,
    staleFlushed: Boolean,
    onClearDatabase: () -> Unit,
    dbCleared: Boolean,
    onFindRssFeed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Goodreads RSS Feed",
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Paste the URL of your Goodreads RSS feed, or use auto-detect " +
                    "to find it after logging in to Goodreads. " +
                    "The URL contains a private access key and is stored encrypted on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onFindRssFeed,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.RssFeed,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text("Auto-detect from Goodreads")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = urlField,
            onValueChange = onUrlChanged,
            label = { Text("Feed URL") },
            placeholder = { Text("https://www.goodreads.com/review/list_rss/…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }

        AnimatedVisibility(
            visible = saved,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = "✓ Saved",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Data Management",
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Books not present in the latest sync are shown faded with a " +
                    "warning badge. Use the button below to remove them immediately " +
                    "instead of waiting for the 30-day auto-cleanup.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onFlushStaleBooks,
            enabled = staleCount > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (staleCount > 0) "Flush $staleCount Missing Book${if (staleCount != 1) "s" else ""}"
                else "No Missing Books",
            )
        }

        AnimatedVisibility(
            visible = staleFlushed,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = "✓ Missing books removed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Clear all cached book data from the local database. " +
                    "Your feed URL will be kept. Books will be reloaded from " +
                    "Goodreads on next refresh.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showClearConfirmation = true },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Clear All Data")
        }

        AnimatedVisibility(
            visible = dbCleared,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = "✓ Database cleared",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    if (showClearConfirmation) {
        ClearDataConfirmationDialog(
            onDismiss = { showClearConfirmation = false },
            onConfirm = {
                showClearConfirmation = false
                onClearDatabase()
            },
        )
    }
}

@Composable
private fun ClearDataConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear all data?") },
        text = {
            Text(
                "This will delete all cached books, series, and sorting " +
                        "preferences. Your feed URL will be kept. Data will be " +
                        "reloaded from Goodreads on next refresh.",
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Clear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun SettingsContentPreview() {
    BooklineTheme(dynamicColor = false) {
        SettingsContent(
            urlField = "https://www.goodreads.com/review/list_rss/12345678?key=abc",
            onUrlChanged = {},
            onSave = {},
            saved = false,
            staleCount = 0,
            onFlushStaleBooks = {},
            staleFlushed = false,
            onClearDatabase = {},
            dbCleared = false,
            onFindRssFeed = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun SettingsContentSavedWithStaleBooksPreview() {
    BooklineTheme(dynamicColor = false) {
        SettingsContent(
            urlField = "https://www.goodreads.com/review/list_rss/12345678?key=abc",
            onUrlChanged = {},
            onSave = {},
            saved = true,
            staleCount = 3,
            onFlushStaleBooks = {},
            staleFlushed = false,
            onClearDatabase = {},
            dbCleared = false,
            onFindRssFeed = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ClearDataConfirmationDialogPreview() {
    BooklineTheme(dynamicColor = false) {
        ClearDataConfirmationDialog(
            onDismiss = {},
            onConfirm = {},
        )
    }
}
