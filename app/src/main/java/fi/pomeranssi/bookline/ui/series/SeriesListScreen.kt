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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import fi.pomeranssi.bookline.ui.components.EmptyContent
import fi.pomeranssi.bookline.ui.components.NoFeedConfiguredContent
import fi.pomeranssi.bookline.ui.components.RefreshableContent
import fi.pomeranssi.bookline.ui.components.SearchField
import fi.pomeranssi.bookline.ui.components.SeriesCard
import fi.pomeranssi.bookline.ui.components.SyncErrorBanner
import fi.pomeranssi.bookline.ui.common.SyncResult

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
                    message = "No series found in your feed.",
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
                    Column {
                        SearchField(
                            value = filterText,
                            onValueChange = { viewModel.filterText.value = it },
                            placeholder = "Filter series…",
                        )
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 8.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val syncError = lastSyncResult
                            if (syncError is SyncResult.Error) {
                                item(key = "__sync_error__") {
                                    SyncErrorBanner(
                                        message = syncError.message,
                                        onRetry = { viewModel.refresh() },
                                    )
                                }
                            }
                            items(
                                items = filteredSeries,
                                key = { it.name },
                            ) { series ->
                                SeriesCard(
                                    series = series,
                                    onClick = { onSeriesClick(series.name) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
