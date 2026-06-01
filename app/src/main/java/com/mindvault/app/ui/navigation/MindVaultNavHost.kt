package com.mindvault.app.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mindvault.app.ui.components.AppDrawer
import com.mindvault.app.ui.screens.archive.ArchiveScreen
import com.mindvault.app.ui.screens.categories.CategoriesScreen
import com.mindvault.app.ui.screens.editor.NoteEditorScreen
import com.mindvault.app.ui.screens.home.HomeFilter
import com.mindvault.app.ui.screens.home.HomeScreen
import com.mindvault.app.ui.screens.home.HomeViewModel
import com.mindvault.app.ui.screens.settings.SettingsScreen
import com.mindvault.app.ui.screens.trash.TrashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

private val enterFromRight = slideInHorizontally(initialOffsetX = { it }) + fadeIn()
private val exitToLeft = slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()
private val enterFromLeft = slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn()
private val exitToRight = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()

@Composable
fun MindVaultNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    initialWidgetAction: String? = null,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val homeViewModel: HomeViewModel = hiltViewModel()

    LaunchedEffect(initialWidgetAction) {
        when (initialWidgetAction) {
            "new_note", "quick_idea" -> navController.navigate(Screen.NoteEditor.createRoute())
            "search" -> homeViewModel.openSearch()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                currentRoute = currentRoute,
                drawerState = drawerState,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onFilterFavorites = {
                    homeViewModel.setActiveFilter(HomeFilter.Favorites)
                },
            )
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = modifier,
            enterTransition = { enterFromRight },
            exitTransition = { exitToLeft },
            popEnterTransition = { enterFromLeft },
            popExitTransition = { exitToRight },
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNoteClick = { noteId ->
                        navController.navigate(Screen.NoteEditor.createRoute(noteId))
                    },
                    onNewNote = {
                        navController.navigate(Screen.NoteEditor.createRoute())
                    },
                    onOpenSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onOpenDrawer = {
                        scope.launch { drawerState.open() }
                    },
                    viewModel = homeViewModel,
                )
            }

            composable(
                route = Screen.NoteEditor.route,
                arguments = listOf(
                    navArgument("noteId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                ),
            ) { backStackEntry ->
                val rawId = backStackEntry.arguments?.getLong("noteId") ?: -1L
                NoteEditorScreen(
                    noteId = if (rawId == -1L) null else rawId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNote = { targetId ->
                        navController.navigate(Screen.NoteEditor.createRoute(targetId))
                    },
                )
            }

            composable(Screen.Archive.route) {
                ArchiveScreen(
                    onNoteClick = { noteId ->
                        navController.navigate(Screen.NoteEditor.createRoute(noteId))
                    },
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Screen.Trash.route) {
                TrashScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Screen.Categories.route) {
                CategoriesScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
