package fi.pomeranssi.bookline.ui.series

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import fi.pomeranssi.bookline.R
import fi.pomeranssi.bookline.domain.model.Series
import fi.pomeranssi.bookline.ui.common.PreviewData
import fi.pomeranssi.bookline.ui.common.SyncResult
import fi.pomeranssi.bookline.ui.components.EmptyContent
import fi.pomeranssi.bookline.ui.components.NoFeedConfiguredContent
import fi.pomeranssi.bookline.ui.components.RefreshableContent
import fi.pomeranssi.bookline.ui.components.SearchField
import fi.pomeranssi.bookline.ui.components.SeriesCard
import fi.pomeranssi.bookline.ui.components.SyncErrorBanner
import fi.pomeranssi.bookline.ui.theme.BooklineTheme

@Composable
fun SeriesListScreen(
    viewModel: SeriesListViewModel,
    onSeriesClick: (seriesName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val lastSyncResult by viewModel.lastSyncResult.collectAsState()

    LifecycleResumeEffect(viewModel) {
        viewModel.checkSync()
        onPauseOrDispose { }
    }

    when (val state = uiState) {
        SeriesListUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        SeriesListUiState.NoFeedConfigured -> {
            NoFeedConfiguredContent(modifier = modifier.fillMaxSize())
        }

        is SeriesListUiState.Success -> {
            val filteredSeries by viewModel.filteredSeries.collectAsState()
            val filterText by viewModel.filterText.collectAsState()

            if (state.series.isEmpty() && !isRefreshing) {
                EmptyContent(
                    message = stringResource(R.string.empty_no_series),
                    modifier = modifier.fillMaxSize(),
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            modifier = Modifier.padding(bottom = 16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    onRefresh = { viewModel.refresh() },
                )
            } else {
                RefreshableContent(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = modifier.fillMaxSize(),
                ) {
                    SeriesListContent(
                        series = filteredSeries,
                        filterText = filterText,
                        onFilterTextChange = { viewModel.filterText.value = it },
                        syncErrorMessage = (lastSyncResult as? SyncResult.Error)?.message,
                        onRetrySync = { viewModel.refresh() },
                        onSeriesClick = onSeriesClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun SeriesListContent(
    series: List<Series>,
    filterText: String,
    onFilterTextChange: (String) -> Unit,
    syncErrorMessage: String?,
    onRetrySync: () -> Unit,
    onSeriesClick: (seriesName: String) -> Unit,
) {
    Column {
        SearchField(
            value = filterText,
            onValueChange = onFilterTextChange,
            placeholder = stringResource(R.string.filter_series_placeholder),
        )
        LazyColumn(
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 8.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (syncErrorMessage != null) {
                item(key = "__sync_error__") {
                    SyncErrorBanner(
                        message = syncErrorMessage,
                        onRetry = onRetrySync,
                    )
                }
            }
            items(
                items = series,
                key = { it.name },
            ) { item ->
                SeriesCard(
                    series = item,
                    onClick = { onSeriesClick(item.name) },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun SeriesListContentPreview() {
    BooklineTheme(dynamicColor = false) {
        SeriesListContent(
            series = PreviewData.seriesList,
            filterText = "",
            onFilterTextChange = {},
            syncErrorMessage = null,
            onRetrySync = {},
            onSeriesClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun SeriesListContentWithErrorPreview() {
    BooklineTheme(dynamicColor = false) {
        SeriesListContent(
            series = PreviewData.seriesList.take(1),
            filterText = "horizon",
            onFilterTextChange = {},
            syncErrorMessage = "Could not reach Goodreads: connection timed out",
            onRetrySync = {},
            onSeriesClick = {},
        )
    }
}
