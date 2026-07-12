package fi.pomeranssi.bookline.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level destinations reachable from the bottom navigation bar.
 */
enum class TopLevelRoute(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Timeline(route = "timeline", label = "Timeline", icon = Icons.Default.Timeline),
    ToRead(route = "to_read", label = "To Read", icon = Icons.Default.Book),
    Series(route = "series", label = "Series", icon = Icons.Default.CollectionsBookmark),
    Library(route = "library", label = "Library", icon = Icons.AutoMirrored.Filled.LibraryBooks),
}
