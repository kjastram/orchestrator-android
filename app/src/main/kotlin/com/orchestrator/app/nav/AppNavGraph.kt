package com.orchestrator.app.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.orchestrator.app.data.repository.AuthRepository
import com.orchestrator.app.ui.login.LoginScreen
import com.orchestrator.app.ui.tasks.TaskEditSheet

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main")
    object TaskEdit : Screen("task_edit/{taskId}") {
        fun go(taskId: String) = "task_edit/$taskId"
        const val NEW = "new"
        const val ARG_TASK_ID = "taskId"
    }
}

@Composable
fun AppNavGraph(authRepository: AuthRepository) {
    val navController = rememberNavController()
    val startDestination = if (authRepository.isLoggedIn()) Screen.Main.route else Screen.Login.route

    // Very subtle cross-fade between destinations. Without an enter transition the
    // incoming screen's first frame is the bare (white) window background, which
    // reads as a hard flash; fading it in over the outgoing screen removes that.
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(180)) },
        exitTransition = { fadeOut(animationSpec = tween(140)) },
        popEnterTransition = { fadeIn(animationSpec = tween(180)) },
        popExitTransition = { fadeOut(animationSpec = tween(140)) }
    ) {

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = hiltViewModel(),
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScaffold(
                onEditTask = { task ->
                    navController.navigate(Screen.TaskEdit.go(task.id))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.TaskEdit.route,
            arguments = listOf(navArgument(Screen.TaskEdit.ARG_TASK_ID) { type = NavType.StringType })
        ) {
            TaskEditSheet(
                viewModel = hiltViewModel(),
                onDone = {
                    navController.popBackStack()
                }
            )
        }
    }
}
