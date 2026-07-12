package fi.pomeranssi.bookline.ui.series

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.pomeranssi.bookline.R
import fi.pomeranssi.bookline.domain.model.Book
import fi.pomeranssi.bookline.ui.common.PreviewData
import fi.pomeranssi.bookline.ui.components.BookCard
import fi.pomeranssi.bookline.ui.theme.BooklineTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    viewModel: SeriesDetailViewModel,
    onNavigateBack: () -> Unit,
    onBookClick: (bookId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRenameDialog by remember { mutableStateOf(false) }

    // Navigate back after rename (the series list will reflect the change)
    LaunchedEffect(Unit) {
        viewModel.renamed.collect {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = (uiState as? SeriesDetailUiState.Success)?.seriesName ?: ""
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.action_rename_series),
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        when (val state = uiState) {
            SeriesDetailUiState.Loading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.padding(top = 2.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            is SeriesDetailUiState.Success -> {
                SeriesDetailContent(
                    seriesName = state.seriesName,
                    aliases = state.aliases,
                    books = state.books,
                    onBookClick = onBookClick,
                    contentPadding =
                        PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = innerPadding.calculateTopPadding() + 8.dp,
                            bottom = innerPadding.calculateBottomPadding() + 8.dp,
                        ),
                )

                if (showRenameDialog) {
                    RenameSeriesDialog(
                        currentName = state.seriesName,
                        onDismiss = { showRenameDialog = false },
                        onConfirm = { newName ->
                            showRenameDialog = false
                            viewModel.renameSeries(newName)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SeriesDetailContent(
    seriesName: String,
    aliases: List<String>,
    books: List<Book>,
    onBookClick: (bookId: String) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (aliases.isNotEmpty()) {
            item(key = "_aliases") {
                Text(
                    text = stringResource(R.string.series_also_known_as, aliases.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        items(
            items = books,
            key = { it.bookId },
        ) { book ->
            val position =
                book.seriesEntries
                    .firstOrNull { it.seriesName == seriesName }
                    ?.position

            val positionLabel =
                position?.let { pos ->
                    if (pos == pos.toLong().toDouble()) {
                        "#${pos.toLong()}"
                    } else {
                        "#$pos"
                    }
                }

            BookCard(
                book =
                    if (positionLabel != null) {
                        book.copy(title = "$positionLabel — ${book.title}")
                    } else {
                        book
                    },
                showSeriesInfo = false,
                onClick = { onBookClick(book.bookId) },
            )
        }
    }
}

@Composable
private fun RenameSeriesDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_series_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.series_name_label)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.trim().isNotBlank() && text.trim() != currentName,
            ) {
                Text(stringResource(R.string.action_rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 560)
@Composable
private fun SeriesDetailContentPreview() {
    BooklineTheme(dynamicColor = false) {
        SeriesDetailContent(
            seriesName = PreviewData.series.name,
            aliases = listOf("The Horizon Saga"),
            books = PreviewData.series.books,
            onBookClick = {},
            contentPadding = PaddingValues(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RenameSeriesDialogPreview() {
    BooklineTheme(dynamicColor = false) {
        RenameSeriesDialog(
            currentName = PreviewData.series.name,
            onDismiss = {},
            onConfirm = {},
        )
    }
}
