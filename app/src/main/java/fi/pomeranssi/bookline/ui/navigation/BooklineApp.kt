package fi.pomeranssi.bookline.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fi.pomeranssi.bookline.R
import fi.pomeranssi.bookline.data.db.BooklineDatabase
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.data.repository.SettingsRepository
import fi.pomeranssi.bookline.ui.detail.BookDetailScreen
import fi.pomeranssi.bookline.ui.detail.BookDetailViewModel
import fi.pomeranssi.bookline.ui.goodreads.GoodreadsScreen
import fi.pomeranssi.bookline.ui.library.LibraryScreen
import fi.pomeranssi.bookline.ui.library.LibraryViewModel
import fi.pomeranssi.bookline.ui.series.SeriesDetailScreen
import fi.pomeranssi.bookline.ui.series.SeriesDetailViewModel
import fi.pomeranssi.bookline.ui.series.SeriesListScreen
import fi.pomeranssi.bookline.ui.series.SeriesListViewModel
import fi.pomeranssi.bookline.ui.settings.SettingsScreen
import fi.pomeranssi.bookline.ui.settings.SettingsViewModel
import fi.pomeranssi.bookline.ui.shelves.ToReadScreen
import fi.pomeranssi.bookline.ui.shelves.ToReadViewModel
import fi.pomeranssi.bookline.ui.timeline.TimelineScreen
import fi.pomeranssi.bookline.ui.timeline.TimelineViewModel
import java.net.URLDecoder
import java.net.URLEncoder

private const val SETTINGS_ROUTE = "settings"
private const val GOODREADS_ROUTE = "goodreads"
private const val BOOK_DETAIL_ROUTE = "book/{bookId}"
private const val SERIES_DETAIL_ROUTE = "series_detail/{seriesName}"

/**
 * Holder for app-level dependencies (repositories + ViewModels).
 */
private class AppDependencies(
    val settingsRepository: SettingsRepository,
    val database: BooklineDatabase,
    val bookRepository: BookRepository,
    val timelineViewModel: TimelineViewModel,
    val toReadViewModel: ToReadViewModel,
    val seriesListViewModel: SeriesListViewModel,
    val libraryViewModel: LibraryViewModel,
)

@Composable
private fun rememberAppDependencies(): AppDependencies {
    val context = LocalContext.current
    return remember {
        val settings = SettingsRepository(context.applicationContext)
        val db = BooklineDatabase.getInstance(context.applicationContext)
        val bookRepo = BookRepository(
            db.bookDao(),
            db.bookSeriesDao(),
            db.seriesInfoDao(),
            settings,
            db.bookSortOverrideDao(),
        )
        AppDependencies(
            settingsRepository = settings,
            database = db,
            bookRepository = bookRepo,
            timelineViewModel = TimelineViewModel(settings, bookRepo),
            toReadViewModel = ToReadViewModel(settings, bookRepo),
            seriesListViewModel = SeriesListViewModel(settings, bookRepo),
            libraryViewModel = LibraryViewModel(settings, bookRepo),
        )
    }
}

// ---------------------------------------------------------------------------
// Main app composable
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooklineApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val deps = rememberAppDependencies()

    var goodreadsUrlOverride by remember { mutableStateOf<String?>(null) }
    var goodreadsAutoDetect by remember { mutableStateOf(false) }

    val isTopLevel = TopLevelRoute.entries.any { it.route == currentDestination?.route }
        || currentDestination?.route == GOODREADS_ROUTE
    val isToReadRoute = currentDestination?.route == TopLevelRoute.ToRead.route
    val isTimelineRoute = currentDestination?.route == TopLevelRoute.Timeline.route
    val reorderMode by deps.toReadViewModel.reorderMode.collectAsState()
    val allCollapsed by deps.timelineViewModel.allCollapsed.collectAsState()

    Scaffold(
        topBar = {
            if (isTopLevel) {
                BooklineTopBar(
                    onGoodreadsClick = {
                        goodreadsUrlOverride = null
                        navController.navigate(GOODREADS_ROUTE) { launchSingleTop = true }
                    },
                    onSettingsClick = {
                        navController.navigate(SETTINGS_ROUTE) { launchSingleTop = true }
                    },
                    showReorderToggle = isToReadRoute,
                    reorderMode = reorderMode,
                    onReorderToggle = { deps.toReadViewModel.toggleReorderMode() },
                    showCollapseToggle = isTimelineRoute,
                    allCollapsed = allCollapsed,
                    onCollapseAll = { deps.timelineViewModel.collapseAll() },
                    onExpandAll = { deps.timelineViewModel.expandAll() },
                )
            }
        },
        bottomBar = {
            if (isTopLevel) {
                BooklineBottomBar(
                    currentDestination = currentDestination,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        BooklineNavHost(
            navController = navController,
            innerPadding = innerPadding,
            deps = deps,
            goodreadsUrlOverride = goodreadsUrlOverride,
            onGoodreadsUrlOverride = { goodreadsUrlOverride = it },
            goodreadsAutoDetect = goodreadsAutoDetect,
            onGoodreadsAutoDetect = { goodreadsAutoDetect = it },
        )
    }
}

// ---------------------------------------------------------------------------
// Bottom navigation bar
// ---------------------------------------------------------------------------

@Composable
private fun BooklineBottomBar(
    currentDestination: NavDestination?,
    onTabSelected: (route: String) -> Unit,
) {
    NavigationBar {
        TopLevelRoute.entries.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any {
                it.route == destination.route
            } == true

            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(destination.route) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                    )
                },
                label = { Text(text = destination.label) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Navigation host with all routes
// ---------------------------------------------------------------------------

@Composable
private fun BooklineNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    deps: AppDependencies,
    goodreadsUrlOverride: String?,
    onGoodreadsUrlOverride: (String?) -> Unit,
    goodreadsAutoDetect: Boolean,
    onGoodreadsAutoDetect: (Boolean) -> Unit,
) {
    fun navigateToBook(bookId: String) {
        navController.navigate("book/$bookId") { launchSingleTop = true }
    }

    fun navigateToSeries(seriesName: String) {
        val encoded = URLEncoder.encode(seriesName, "UTF-8")
        navController.navigate("series_detail/$encoded") { launchSingleTop = true }
    }

    NavHost(
        navController = navController,
        startDestination = TopLevelRoute.Timeline.route,
    ) {
        composable(TopLevelRoute.Timeline.route) {
            TimelineScreen(
                viewModel = deps.timelineViewModel,
                onBookClick = ::navigateToBook,
                modifier = Modifier.padding(innerPadding),
            )
        }
        composable(TopLevelRoute.ToRead.route) {
            ToReadScreen(
                viewModel = deps.toReadViewModel,
                onBookClick = ::navigateToBook,
                modifier = Modifier.padding(innerPadding),
            )
        }
        composable(TopLevelRoute.Series.route) {
            SeriesListScreen(
                viewModel = deps.seriesListViewModel,
                onSeriesClick = ::navigateToSeries,
                modifier = Modifier.padding(innerPadding),
            )
        }
        composable(TopLevelRoute.Library.route) {
            LibraryScreen(
                viewModel = deps.libraryViewModel,
                onBookClick = ::navigateToBook,
                modifier = Modifier.padding(innerPadding),
            )
        }
        composable(GOODREADS_ROUTE) {
            val urlOverride = goodreadsUrlOverride
            val autoDetect = goodreadsAutoDetect
            onGoodreadsUrlOverride(null)
            onGoodreadsAutoDetect(false)
            val feedUrl by deps.settingsRepository.feedUrl.collectAsState()
            val isFeedConfigured = feedUrl.isNotBlank()
            GoodreadsScreen(
                initialUrl = urlOverride ?: "https://www.goodreads.com",
                modifier = Modifier.padding(innerPadding),
                onRssFeedDetected = if (!isFeedConfigured || autoDetect) { url ->
                    deps.settingsRepository.saveFeedUrl(url)
                } else null,
                autoDetect = autoDetect,
            )
        }
        composable(SETTINGS_ROUTE) {
            val viewModel = remember {
                SettingsViewModel(deps.settingsRepository, deps.database)
            }
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onFindRssFeed = {
                    onGoodreadsAutoDetect(true)
                    navController.navigate(GOODREADS_ROUTE) { launchSingleTop = true }
                },
            )
        }
        composable(
            route = BOOK_DETAIL_ROUTE,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: return@composable
            val viewModel = remember(bookId) {
                BookDetailViewModel(deps.bookRepository, bookId)
            }
            BookDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenGoodreads = { url ->
                    onGoodreadsUrlOverride(url)
                    navController.navigate(GOODREADS_ROUTE) { launchSingleTop = true }
                },
                onSeriesClick = ::navigateToSeries,
            )
        }
        composable(
            route = SERIES_DETAIL_ROUTE,
            arguments = listOf(navArgument("seriesName") { type = NavType.StringType }),
        ) { backStackEntry ->
            val encodedName =
                backStackEntry.arguments?.getString("seriesName") ?: return@composable
            val seriesName = URLDecoder.decode(encodedName, "UTF-8")
            val viewModel = remember(seriesName) {
                SeriesDetailViewModel(deps.bookRepository, seriesName)
            }
            SeriesDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onBookClick = ::navigateToBook,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Top app bar
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BooklineTopBar(
    onGoodreadsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    showReorderToggle: Boolean = false,
    reorderMode: Boolean = false,
    onReorderToggle: () -> Unit = {},
    showCollapseToggle: Boolean = false,
    allCollapsed: Boolean = false,
    onCollapseAll: () -> Unit = {},
    onExpandAll: () -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Bookline") },
        actions = {
            if (showCollapseToggle) {
                IconButton(onClick = { if (allCollapsed) onExpandAll() else onCollapseAll() }) {
                    Icon(
                        imageVector = if (allCollapsed) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
                        contentDescription = if (allCollapsed) "Expand all" else "Collapse all",
                    )
                }
            }
            if (showReorderToggle) {
                IconButton(onClick = onReorderToggle) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = if (reorderMode) "Exit reorder mode" else "Reorder list",
                        tint = if (reorderMode) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            LocalContentColor.current
                        },
                    )
                }
            }
            IconButton(onClick = onGoodreadsClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_goodreads),
                    contentDescription = "Goodreads",
                    modifier = Modifier.size(24.dp),
                )
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        menuExpanded = false
                        onSettingsClick()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                        )
                    },
                )
            }
        },
    )
}

