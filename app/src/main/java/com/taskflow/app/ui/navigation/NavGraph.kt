package com.taskflow.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.taskflow.app.ui.screens.HomeScreen
import com.taskflow.app.ui.screens.SettingsScreen
import com.taskflow.app.ui.viewmodel.SettingsViewModel
import com.taskflow.app.ui.viewmodel.TaskViewModel
import com.taskflow.app.util.TaskExportHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Settings : Screen("settings")
}

@Composable
fun TaskFlowNavGraph(
    navController: NavHostController,
    taskViewModel: TaskViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val taskUiState by taskViewModel.uiState.collectAsState()
    val settingsUiState by settingsViewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                uiState = taskUiState,
                onAddTask = { taskViewModel.addTask(it) },
                onUpdateTask = { taskViewModel.updateTask(it) },
                onToggleCompletion = { taskViewModel.toggleTaskCompletion(it) },
                onDeleteTask = { taskViewModel.deleteTask(it) },
                onToggleSubtask = { task, index, isDone ->
                    taskViewModel.toggleSubtask(task, index, isDone)
                },
                onFilterChange = { taskViewModel.setFilter(it) },
                onSearchQueryChange = { taskViewModel.setSearchQuery(it) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                defaultVoiceLanguage = settingsUiState.defaultVoiceLanguage,
                onExportRequested = { format ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val snapshot = taskViewModel.repository.getAllTasksSnapshot()
                        if (format == "csv") {
                            val file = TaskExportHelper.exportTasksToCsv(context, snapshot)
                            if (file != null) {
                                TaskExportHelper.shareFile(context, file, "text/csv")
                            }
                        } else {
                            val file = TaskExportHelper.exportTasksToMarkdown(context, snapshot)
                            if (file != null) {
                                TaskExportHelper.shareFile(context, file, "text/markdown")
                            }
                        }
                    }
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                uiState = settingsUiState,
                onNavigateBack = { navController.popBackStack() },
                onMorningNotificationToggle = { settingsViewModel.setMorningNotification(it) },
                onEveningNotificationToggle = { settingsViewModel.setEveningNotification(it) },
                onWeeklyRetroToggle = { settingsViewModel.setWeeklyRetro(it) },
                onMorningTimeChange = { h, m -> settingsViewModel.setMorningTime(h, m) },
                onEveningTimeChange = { h, m -> settingsViewModel.setEveningTime(h, m) },
                onVoiceLanguageChange = { settingsViewModel.setDefaultVoiceLanguage(it) },
                onThemeModeChange = { settingsViewModel.setThemeMode(it) },
                onGeofenceToggle = { settingsViewModel.setGeofenceEnabled(it) }
            )
        }
    }
}
