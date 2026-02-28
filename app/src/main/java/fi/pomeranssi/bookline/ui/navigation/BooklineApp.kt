package fi.pomeranssi.bookline.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fi.pomeranssi.bookline.data.db.BooklineDatabase
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.data.repository.SettingsRepository
import fi.pomeranssi.bookline.ui.components.EmptyContent
import fi.pomeranssi.bookline.ui.detail.BookDetailScreen
import fi.pomeranssi.bookline.ui.detail.BookDetailViewModel
import fi.pomeranssi.bookline.ui.goodreads.GoodreadsScreen
import fi.pomeranssi.bookline.ui.settings.SettingsScreen
import fi.pomeranssi.bookline.ui.settings.SettingsViewModel
import fi.pomeranssi.bookline.ui.shelves.ToReadScreen
import fi.pomeranssi.bookline.ui.shelves.ToReadViewModel
import fi.pomeranssi.bookline.ui.series.SeriesDetailScreen
import fi.pomeranssi.bookline.ui.series.SeriesDetailViewModel
import fi.pomeranssi.bookline.ui.series.SeriesListScreen
import fi.pomeranssi.bookline.ui.series.SeriesListViewModel
import fi.pomeranssi.bookline.ui.timeline.TimelineScreen
import fi.pomeranssi.bookline.ui.timeline.TimelineViewModel

private const val SETTINGS_ROUTE = "settings"
private const val BOOK_DETAIL_ROUTE = "book/{bookId}"
private const val SERIES_DETAIL_ROUTE = "series_detail/{seriesName}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooklineApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context.applicationContext) }
    val database = remember { BooklineDatabase.getInstance(context.applicationContext) }
    val bookRepository = remember { BookRepository(database.bookDao(), database.bookSeriesDao(), settingsRepository) }
    val timelineViewModel = remember { TimelineViewModel(settingsRepository, bookRepository) }
    val toReadViewModel = remember { ToReadViewModel(bookRepository) }
    val seriesListViewModel = remember { SeriesListViewModel(settingsRepository, bookRepository) }

    // URL override for navigating to a specific Goodreads page from book details
    var goodreadsUrlOverride by remember { mutableStateOf<String?>(null) }

    // Determine whether we are on a top-level tab (show bottom bar + top bar)
    val isTopLevel = TopLevelRoute.entries.any { it.route == currentDestination?.route }

    Scaffold(
        topBar = {
            if (isTopLevel) {
                BooklineTopBar(
                    onSettingsClick = {
                        navController.navigate(SETTINGS_ROUTE) {
                            launchSingleTop = true
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    TopLevelRoute.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == destination.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
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
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelRoute.Timeline.route,
        ) {
            composable(TopLevelRoute.Timeline.route) {
                TimelineScreen(
                    viewModel = timelineViewModel,
                    onBookClick = { bookId ->
                        navController.navigate("book/$bookId") {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            composable(TopLevelRoute.ToRead.route) {
                ToReadScreen(
                    viewModel = toReadViewModel,
                    onBookClick = { bookId ->
                        navController.navigate("book/$bookId") {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            composable(TopLevelRoute.Series.route) {
                SeriesListScreen(
                    viewModel = seriesListViewModel,
                    onSeriesClick = { seriesName ->
                        navController.navigate("series_detail/${java.net.URLEncoder.encode(seriesName, "UTF-8")}") {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            composable(TopLevelRoute.Goodreads.route) {
                val urlOverride = goodreadsUrlOverride
                goodreadsUrlOverride = null
                GoodreadsScreen(
                    initialUrl = urlOverride ?: "https://www.goodreads.com",
                    modifier = Modifier.padding(innerPadding),
                )
            }
            composable(SETTINGS_ROUTE) {
                val viewModel = remember { SettingsViewModel(settingsRepository) }
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                route = BOOK_DETAIL_ROUTE,
                arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId") ?: return@composable
                val viewModel = remember(bookId) {
                    BookDetailViewModel(bookRepository, bookId)
                }
                BookDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenGoodreads = { url ->
                        goodreadsUrlOverride = url
                        navController.navigate(TopLevelRoute.Goodreads.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = SERIES_DETAIL_ROUTE,
                arguments = listOf(navArgument("seriesName") { type = NavType.StringType }),
            ) { backStackEntry ->
                val encodedName = backStackEntry.arguments?.getString("seriesName") ?: return@composable
                val seriesName = java.net.URLDecoder.decode(encodedName, "UTF-8")
                val viewModel = remember(seriesName) {
                    SeriesDetailViewModel(bookRepository, seriesName)
                }
                SeriesDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onBookClick = { bookId ->
                        navController.navigate("book/$bookId") {
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BooklineTopBar(
    onSettingsClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Bookline") },
        actions = {
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

