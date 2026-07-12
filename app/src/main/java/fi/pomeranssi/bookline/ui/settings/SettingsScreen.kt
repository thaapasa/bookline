package fi.pomeranssi.bookline.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import fi.pomeranssi.bookline.R
import fi.pomeranssi.bookline.ui.theme.BooklineTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onFindRssFeed: () -> Unit = {},
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
                title = { Text(stringResource(R.string.settings_title)) },
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
        val context = LocalContext.current
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
            currentLanguageTag =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    currentAppLanguageTag(context)
                } else {
                    null
                },
            onLanguageSelected = { tag ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    setAppLanguage(context, tag)
                }
            },
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
    currentLanguageTag: String?,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        if (currentLanguageTag != null) {
            LanguageDropdown(
                currentLanguageTag = currentLanguageTag,
                onLanguageSelected = onLanguageSelected,
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = stringResource(R.string.settings_feed_section),
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.settings_feed_description),
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
            Text(stringResource(R.string.settings_autodetect))
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = urlField,
            onValueChange = onUrlChanged,
            label = { Text(stringResource(R.string.settings_feed_url_label)) },
            placeholder = { Text(stringResource(R.string.settings_feed_url_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_save))
        }

        AnimatedVisibility(
            visible = saved,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = stringResource(R.string.settings_saved),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.settings_data_section),
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.settings_stale_description),
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
                if (staleCount > 0) {
                    pluralStringResource(R.plurals.settings_flush_missing_books, staleCount, staleCount)
                } else {
                    stringResource(R.string.settings_no_missing_books)
                },
            )
        }

        AnimatedVisibility(
            visible = staleFlushed,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = stringResource(R.string.settings_missing_books_removed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.settings_clear_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showClearConfirmation = true },
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_clear_all_data))
        }

        AnimatedVisibility(
            visible = dbCleared,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = stringResource(R.string.settings_database_cleared),
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

/**
 * In-app language selection reads/writes the same per-app locale store as the
 * Android 13+ system language settings, so both stay in sync.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun currentAppLanguageTag(context: Context): String {
    val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
    return if (locales.isEmpty) "" else locales[0].language
}

/** Empty [tag] resets the app to follow the system language. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun setAppLanguage(
    context: Context,
    tag: String,
) {
    context.getSystemService(LocaleManager::class.java).applicationLocales =
        if (tag.isEmpty()) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(tag)
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    currentLanguageTag: String,
    onLanguageSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options =
        listOf(
            "" to stringResource(R.string.settings_language_system),
            "en" to stringResource(R.string.language_english),
            "fi" to stringResource(R.string.language_finnish),
        )
    val currentLabel =
        options.firstOrNull { (tag, _) -> tag == currentLanguageTag }?.second
            ?: options.first().second

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.settings_language_section)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (tag, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onLanguageSelected(tag)
                    },
                )
            }
        }
    }
}

@Composable
private fun ClearDataConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clear_dialog_title)) },
        text = {
            Text(stringResource(R.string.clear_dialog_text))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text(stringResource(R.string.action_clear))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
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
            currentLanguageTag = "",
            onLanguageSelected = {},
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
            currentLanguageTag = "fi",
            onLanguageSelected = {},
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
