package com.taskflow.app.data.repository

import com.taskflow.app.data.local.TaskDao
import com.taskflow.app.data.local.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TaskRepository(private val taskDao: TaskDao) {
    
    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()
    fun getPendingTasks(): Flow<List<TaskEntity>> = taskDao.getPendingTasks()
    fun getCompletedTasks(): Flow<List<TaskEntity>> = taskDao.getCompletedTasks()
    fun getTasksByCategory(category: String): Flow<List<TaskEntity>> = taskDao.getTasksByCategory(category)
    
    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)
    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)
    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)
    
    suspend fun toggleTaskCompletion(task: TaskEntity) {
        val now = if (!task.isCompleted) System.currentTimeMillis() else null
        taskDao.updateTaskCompletion(task.id, !task.isCompleted, now)
        
        // Handle recurrence if completed
        if (!task.isCompleted && task.recurrence != "None") {
            createNextRecurringTask(task)
        }
    }

    private suspend fun createNextRecurringTask(task: TaskEntity) {
        val calendar = Calendar.getInstance()
        val baseDate = task.dueDate ?: System.currentTimeMillis()
        calendar.timeInMillis = baseDate

        when (task.recurrence) {
            "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "Weekdays" -> {
                do {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                } while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || 
                         calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            }
            "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }

        val nextTask = task.copy(
            id = 0,
            isCompleted = false,
            completedAt = null,
            createdAt = System.currentTimeMillis(),
            dueDate = calendar.timeInMillis
        )
        taskDao.insertTask(nextTask)
    }
    
    suspend fun deleteAllCompletedTasks() = taskDao.deleteAllCompletedTasks()
    suspend fun getPendingTaskCount(): Int = taskDao.getPendingTaskCount()
    suspend fun getCompletedTaskCountSince(since: Long): Int = taskDao.getCompletedTaskCountSince(since)
    suspend fun getPendingTasksList(): List<TaskEntity> = taskDao.getPendingTasksList()
    suspend fun getCompletedTasksSince(since: Long): List<TaskEntity> = taskDao.getCompletedTasksSince(since)
    suspend fun getAllCompletedTasksList(): List<TaskEntity> = taskDao.getAllCompletedTasksList()
    suspend fun getAllTasksSnapshot(): List<TaskEntity> = taskDao.getAllTasksSnapshot()
}
