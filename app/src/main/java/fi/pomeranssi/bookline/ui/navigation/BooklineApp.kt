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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.data.repository.SettingsRepository
import fi.pomeranssi.bookline.ui.goodreads.GoodreadsScreen
import fi.pomeranssi.bookline.ui.settings.SettingsScreen
import fi.pomeranssi.bookline.ui.settings.SettingsViewModel
import fi.pomeranssi.bookline.ui.shelves.ToReadScreen
import fi.pomeranssi.bookline.ui.timeline.TimelineScreen
import fi.pomeranssi.bookline.ui.timeline.TimelineViewModel

private const val SETTINGS_ROUTE = "settings"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooklineApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context.applicationContext) }
    val bookRepository = remember { BookRepository() }
    val timelineViewModel = remember { TimelineViewModel(settingsRepository, bookRepository) }

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
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelRoute.Timeline.route) {
                TimelineScreen(viewModel = timelineViewModel)
            }
            composable(TopLevelRoute.ToRead.route) {
                ToReadScreen()
            }
            composable(TopLevelRoute.Goodreads.route) {
                GoodreadsScreen()
            }
            composable(SETTINGS_ROUTE) {
                val viewModel = remember { SettingsViewModel(settingsRepository) }
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
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

