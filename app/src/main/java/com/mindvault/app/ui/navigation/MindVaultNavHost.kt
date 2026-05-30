package com.mindvault.app.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mindvault.app.ui.screens.editor.NoteEditorScreen
import com.mindvault.app.ui.screens.home.HomeScreen

private val enterFromRight = slideInHorizontally(initialOffsetX = { it }) + fadeIn()
private val exitToLeft = slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()
private val enterFromLeft = slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn()
private val exitToRight = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()

@Composable
fun MindVaultNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
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
            )
        }
    }
}
