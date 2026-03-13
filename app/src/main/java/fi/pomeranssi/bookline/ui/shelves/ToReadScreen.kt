package fi.pomeranssi.bookline.ui.shelves

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow
import fi.pomeranssi.bookline.domain.model.ToReadBookItem
import fi.pomeranssi.bookline.ui.components.BookCard
import fi.pomeranssi.bookline.ui.components.EmptyContent
import fi.pomeranssi.bookline.ui.components.NoFeedConfiguredContent
import fi.pomeranssi.bookline.ui.components.RefreshableContent
import fi.pomeranssi.bookline.ui.components.SyncErrorBanner
import fi.pomeranssi.bookline.ui.common.SyncResult
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ToReadScreen(
    viewModel: ToReadViewModel,
    onBookClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items by viewModel.books.collectAsState(initial = emptyList())
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val reorderMode by viewModel.reorderMode.collectAsState()
    val lastSyncResult by viewModel.lastSyncResult.collectAsState()

    if (!viewModel.isFeedConfigured) {
        NoFeedConfiguredContent(modifier = modifier)
    } else if (items.isEmpty() && !isRefreshing) {
        EmptyContent(
            message = "Your reading list will appear here.",
            modifier = modifier,
            icon = {
                Icon(
                    imageVector = Icons.Default.Book,
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
            modifier = modifier,
        ) {
            if (reorderMode) {
                ReorderableToReadList(
                    items = items,
                    onBookMoved = { bookId, reorderedItems ->
                        viewModel.onBookMoved(bookId, reorderedItems)
                    },
                    onBookClick = onBookClick,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                    val syncError = lastSyncResult
                    if (syncError is SyncResult.Error) {
                        item(key = "__sync_error__") {
                            SyncErrorBanner(
                                message = syncError.message,
                                onRetry = { viewModel.refresh() },
                            )
                        }
                    }
                    items(items = items, key = { it.book.bookId }) { item ->
                        BookCard(book = item.book, onClick = { onBookClick(item.book.bookId) })
                    }
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ReorderableToReadList(
    items: List<ToReadBookItem>,
    onBookMoved: (bookId: String, reorderedItems: List<ToReadBookItem>) -> Unit,
    onBookClick: (String) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current

    // Local mutable copy for drag reordering visual feedback
    var localItems by remember { mutableStateOf(items) }
    var isReordering by remember { mutableStateOf(false) }

    // Sync from upstream only when not dragging
    LaunchedEffect(items, isReordering) {
        if (!isReordering) {
            localItems = items
        }
    }

    // Track which book is being dragged
    var draggedBookId by remember { mutableStateOf<String?>(null) }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Use key-based lookup to avoid index offset issues with spacer items
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey = to.key as? String ?: return@rememberReorderableLazyListState
        localItems = localItems.toMutableList().apply {
            val fromIdx = indexOfFirst { it.book.bookId == fromKey }
            val toIdx = indexOfFirst { it.book.bookId == toKey }
            if (fromIdx >= 0 && toIdx >= 0) {
                add(toIdx, removeAt(fromIdx))
            }
        }
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }
        items(items = localItems, key = { it.book.bookId }) { item ->
            ReorderableItem(reorderableState, key = item.book.bookId) { isDragging ->
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 4.dp else 0.dp,
                    label = "dragElevation",
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BookCard(
                        book = item.book,
                        onClick = { onBookClick(item.book.bookId) },
                        modifier = Modifier
                            .weight(1f)
                            .shadow(elevation, shape = MaterialTheme.shapes.medium),
                    )
                    IconButton(
                        modifier = Modifier.draggableHandle(
                            onDragStarted = {
                                isReordering = true
                                draggedBookId = item.book.bookId
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                            },
                            onDragStopped = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                draggedBookId?.let { bookId ->
                                    onBookMoved(bookId, localItems)
                                }
                                draggedBookId = null
                                isReordering = false
                            },
                        ),
                        onClick = {},
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DragHandle,
                            contentDescription = "Reorder",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(4.dp)) }
    }
}
