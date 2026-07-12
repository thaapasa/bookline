package fi.pomeranssi.bookline.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector
import fi.pomeranssi.bookline.R

/**
 * Top-level destinations reachable from the bottom navigation bar.
 */
enum class TopLevelRoute(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Timeline(route = "timeline", labelRes = R.string.nav_timeline, icon = Icons.Default.Timeline),
    ToRead(route = "to_read", labelRes = R.string.nav_to_read, icon = Icons.Default.Book),
    Series(route = "series", labelRes = R.string.nav_series, icon = Icons.Default.CollectionsBookmark),
    Library(route = "library", labelRes = R.string.nav_library, icon = Icons.AutoMirrored.Filled.LibraryBooks),
}
