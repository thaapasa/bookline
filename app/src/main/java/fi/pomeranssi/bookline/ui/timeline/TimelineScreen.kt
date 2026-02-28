package fi.pomeranssi.bookline.ui.timeline

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import fi.pomeranssi.bookline.ui.components.BookCard
import fi.pomeranssi.bookline.ui.components.EmptyContent
import fi.pomeranssi.bookline.ui.components.RefreshableContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    onBookClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    LifecycleResumeEffect(viewModel) {
        viewModel.checkSync()
        onPauseOrDispose { }
    }

    when (val state = uiState) {
        is TimelineUiState.NoFeedConfigured -> EmptyContent(
            message = "Set up your Goodreads RSS feed in Settings to see your timeline.",
            modifier = modifier,
            icon = {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    modifier = Modifier.padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
        )

        is TimelineUiState.Loading,
        is TimelineUiState.Success,
            -> {
            val sections = (state as? TimelineUiState.Success)?.sections.orEmpty()
            if (sections.isEmpty() && !isRefreshing) {
                EmptyContent(
                    message = "No books found in your feed.",
                    modifier = modifier,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            modifier = Modifier.padding(bottom = 16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
            } else {
                RefreshableContent(
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = modifier,
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }
                        items(
                            items = sections,
                            key = { section ->
                                when (section) {
                                    is TimelineSection.Header -> section.key
                                    is TimelineSection.BookItem -> section.book.bookId
                                }
                            },
                        ) { section ->
                            when (section) {
                                is TimelineSection.Header -> SectionHeader(
                                    header = section,
                                    onToggle = {
                                        if (section.level == SectionLevel.Year) {
                                            viewModel.toggleYear(
                                                section.key,
                                                section.childKeys,
                                            )
                                        } else {
                                            viewModel.toggleSection(section.key)
                                        }
                                    },
                                )
                                is TimelineSection.BookItem -> BookCard(
                                    book = section.book,
                                    onClick = { onBookClick(section.book.bookId) },
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(4.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    header: TimelineSection.Header,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (header.isCollapsed) -90f else 0f,
        label = "chevron",
    )

    val style = when (header.level) {
        SectionLevel.Top -> MaterialTheme.typography.titleLarge
        SectionLevel.Year -> MaterialTheme.typography.titleLarge
        SectionLevel.Month -> MaterialTheme.typography.titleMedium
    }

    val topPadding = when (header.level) {
        SectionLevel.Top, SectionLevel.Year -> 12.dp
        SectionLevel.Month -> 4.dp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(top = topPadding, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = header.title,
            style = style,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${header.bookCount}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 4.dp),
        )
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = if (header.isCollapsed) "Expand" else "Collapse",
            modifier = Modifier
                .size(24.dp)
                .rotate(rotation),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

