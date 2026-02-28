package fi.pomeranssi.bookline.ui.series

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import fi.pomeranssi.bookline.ui.components.EmptyContent
import fi.pomeranssi.bookline.ui.components.SeriesCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesListScreen(
    viewModel: SeriesListViewModel,
    onSeriesClick: (seriesName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // Re-check sync when returning to this screen
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.currentState.let { state ->
            if (state.isAtLeast(Lifecycle.State.RESUMED)) {
                viewModel.checkSync()
            }
        }
    }

    when (val state = uiState) {
        SeriesListUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.padding(top = 2.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        SeriesListUiState.NoFeedConfigured -> {
            EmptyContent(
                message = "No feed configured.\nGo to Settings to add your Goodreads RSS URL.",
                modifier = modifier.fillMaxSize(),
            )
        }

        is SeriesListUiState.Success -> {
            val filteredSeries by viewModel.filteredSeries.collectAsState()
            val filterText by viewModel.filterText.collectAsState()

            if (state.series.isEmpty() && !isRefreshing) {
                EmptyContent(
                    message = "No series found.\nBooks with series info in their title will appear here.",
                    modifier = modifier.fillMaxSize(),
                )
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item(key = "_filter") {
                            OutlinedTextField(
                                value = filterText,
                                onValueChange = { viewModel.filterText.value = it },
                                placeholder = { Text("Filter series…") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                    )
                                },
                                trailingIcon = {
                                    if (filterText.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.filterText.value = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear filter",
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
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
