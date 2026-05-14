package com.orchestrator.app.nav

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
import com.orchestrator.app.ui.tasks.TaskListScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Tasks : Screen("tasks")
    object TaskEdit : Screen("task_edit/{taskId}") {
        fun go(taskId: String) = "task_edit/$taskId"
        const val NEW = "new"
        const val ARG_TASK_ID = "taskId"
    }
}

@Composable
fun AppNavGraph(authRepository: AuthRepository) {
    val navController = rememberNavController()
    val startDestination = if (authRepository.isLoggedIn()) Screen.Tasks.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = hiltViewModel(),
                onLoginSuccess = {
                    navController.navigate(Screen.Tasks.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Tasks.route) {
            TaskListScreen(
                viewModel = hiltViewModel(),
                onAddTask = {
                    navController.navigate(Screen.TaskEdit.go(Screen.TaskEdit.NEW))
                },
                onEditTask = { task ->
                    navController.navigate(Screen.TaskEdit.go(task.id))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Tasks.route) { inclusive = true }
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
