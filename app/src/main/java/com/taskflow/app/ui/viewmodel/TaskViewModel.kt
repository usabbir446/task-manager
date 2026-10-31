package com.taskflow.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taskflow.app.data.local.AppDatabase
import com.taskflow.app.data.local.TaskEntity
import com.taskflow.app.data.repository.TaskRepository
import com.taskflow.app.util.TaskAlarmScheduler
import com.taskflow.app.widget.TaskWidgetProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class TaskUiState(
    val allTasks: List<TaskEntity> = emptyList(),
    val pendingTasks: List<TaskEntity> = emptyList(),
    val completedTasks: List<TaskEntity> = emptyList(),
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val searchQuery: String = "",
    val streakDays: Int = 1,
    val completedTodayCount: Int = 0,
    val peakProductivityWindow: String = "10 AM - 12 PM",
    val isLoading: Boolean = true
)

enum class TaskFilter { ALL, PENDING, COMPLETED }

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    
    val repository: TaskRepository
    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()
    private val _searchQuery = MutableStateFlow("")

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())
        
        viewModelScope.launch {
            combine(
                repository.getAllTasks(),
                repository.getPendingTasks(),
                repository.getCompletedTasks()
            ) { all, pending, completed ->
                Triple(all, pending, completed)
            }.combine(_searchQuery) { (all, pending, completed), query ->
                val filteredAll = if (query.isBlank()) all else all.filter { 
                    it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
                }
                val filteredPending = if (query.isBlank()) pending else pending.filter { 
                    it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
                }
                val filteredCompleted = if (query.isBlank()) completed else completed.filter { 
                    it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
                }

                // Calculate streaks and analytics
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                }.timeInMillis
                val completedToday = completed.filter { (it.completedAt ?: 0) >= todayStart }

                val (streak, peakHour) = calculateAnalytics(completed)

                TaskUiState(
                    allTasks = filteredAll,
                    pendingTasks = filteredPending,
                    completedTasks = filteredCompleted,
                    selectedFilter = _uiState.value.selectedFilter,
                    searchQuery = query,
                    streakDays = streak,
                    completedTodayCount = completedToday.size,
                    peakProductivityWindow = peakHour,
                    isLoading = false
                )
            }.collect { state -> 
                _uiState.value = state 
            }
        }
    }

    private fun calculateAnalytics(completed: List<TaskEntity>): Pair<Int, String> {
        if (completed.isEmpty()) return Pair(1, "10 AM - 12 PM")

        // Group by hour
        val hourCounts = mutableMapOf<Int, Int>()
        val cal = Calendar.getInstance()
        val daysWithCompletions = mutableSetOf<String>()

        for (t in completed) {
            val stamp = t.completedAt ?: continue
            cal.timeInMillis = stamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hourCounts[hour] = (hourCounts[hour] ?: 0) + 1
            val dayKey = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
            daysWithCompletions.add(dayKey)
        }

        val topHour = hourCounts.maxByOrNull { it.value }?.key ?: 10
        val nextHour = (topHour + 2) % 24
        val formatTop = if (topHour == 0) "12 AM" else if (topHour > 12) "${topHour - 12} PM" else "$topHour AM"
        val formatNext = if (nextHour == 0) "12 AM" else if (nextHour > 12) "${nextHour - 12} PM" else "$nextHour AM"
        val peakWindow = "$formatTop - $formatNext"

        val streak = maxOf(1, daysWithCompletions.size)
        return Pair(streak, peakWindow)
    }

    fun setFilter(filter: TaskFilter) { _uiState.update { it.copy(selectedFilter = filter) } }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun addTask(task: TaskEntity) {
        viewModelScope.launch {
            val id = repository.insertTask(task)
            if (task.dueDate != null) {
                TaskAlarmScheduler.scheduleTaskReminder(
                    getApplication(), id, task.title, task.dueDate
                )
            }
            TaskWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
            TaskWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            TaskAlarmScheduler.cancelTaskReminder(getApplication(), task.id)
            TaskWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task)
            TaskWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun toggleSubtask(task: TaskEntity, subtaskIndex: Int, isDone: Boolean) {
        viewModelScope.launch {
            val parts = task.subTasks.split(",").toMutableList()
            if (subtaskIndex in parts.indices) {
                val tokens = parts[subtaskIndex].split(":")
                val title = tokens[0]
                parts[subtaskIndex] = "$title:$isDone"
                val updatedTask = task.copy(subTasks = parts.joinToString(","))
                repository.updateTask(updatedTask)
            }
        }
    }

    fun deleteAllCompleted() {
        viewModelScope.launch {
            repository.deleteAllCompletedTasks()
            TaskWidgetProvider.updateAllWidgets(getApplication())
        }
    }
}
