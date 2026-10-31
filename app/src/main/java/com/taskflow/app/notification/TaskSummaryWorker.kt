package com.taskflow.app.notification

import android.content.Context
import androidx.work.*
import com.taskflow.app.data.local.AppDatabase
import com.taskflow.app.data.repository.TaskRepository
import java.util.Calendar
import java.util.concurrent.TimeUnit

class TaskSummaryWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val type = inputData.getString("summary_type") ?: "morning"
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TaskRepository(database.taskDao())

        val pendingTasks = repository.getPendingTasksList()
        val pendingCount = pendingTasks.size
        val pendingNames = pendingTasks.map { it.title }

        when (type) {
            "morning" -> {
                val yesterday = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                }.timeInMillis
                val completedYesterday = repository.getCompletedTaskCountSince(yesterday)
                val topTask = pendingTasks.firstOrNull()?.let { Pair(it.id, it.title) }
                NotificationHelper.showMorningSummary(applicationContext, pendingCount, completedYesterday, topTask)
            }
            "evening" -> {
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                }.timeInMillis
                val completedToday = repository.getCompletedTaskCountSince(todayStart)
                NotificationHelper.showEveningSummary(applicationContext, completedToday, pendingCount, pendingNames)
            }
            "weekly" -> {
                val oneWeekAgo = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                }.timeInMillis
                val completedThisWeek = repository.getCompletedTasksSince(oneWeekAgo)
                val topCategory = completedThisWeek.groupBy { it.category }
                    .maxByOrNull { it.value.size }?.key ?: "General"
                NotificationHelper.showWeeklyRetro(applicationContext, completedThisWeek.size, topCategory, pendingCount)
            }
        }
        return Result.success()
    }

    companion object {
        const val MORNING_WORK_NAME = "morning_task_summary"
        const val EVENING_WORK_NAME = "evening_task_summary"
        const val WEEKLY_WORK_NAME = "weekly_retrospective_summary"

        fun scheduleMorningNotification(context: Context, hour: Int, minute: Int) {
            val delay = calculateDelay(hour, minute)
            val inputData = workDataOf("summary_type" to "morning")
            val workRequest = PeriodicWorkRequestBuilder<TaskSummaryWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("morning_summary")
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(MORNING_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, workRequest)
        }

        fun scheduleEveningNotification(context: Context, hour: Int, minute: Int) {
            val delay = calculateDelay(hour, minute)
            val inputData = workDataOf("summary_type" to "evening")
            val workRequest = PeriodicWorkRequestBuilder<TaskSummaryWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("evening_summary")
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(EVENING_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, workRequest)
        }

        fun scheduleWeeklyRetro(context: Context) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 19)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            if (target.before(now)) {
                target.add(Calendar.WEEK_OF_YEAR, 1)
            }
            val initialDelay = target.timeInMillis - now.timeInMillis
            val inputData = workDataOf("summary_type" to "weekly")
            val workRequest = PeriodicWorkRequestBuilder<TaskSummaryWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("weekly_summary")
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WEEKLY_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, workRequest)
        }

        fun cancelMorningNotification(context: Context) { WorkManager.getInstance(context).cancelUniqueWork(MORNING_WORK_NAME) }
        fun cancelEveningNotification(context: Context) { WorkManager.getInstance(context).cancelUniqueWork(EVENING_WORK_NAME) }
        fun cancelWeeklyRetro(context: Context) { WorkManager.getInstance(context).cancelUniqueWork(WEEKLY_WORK_NAME) }

        private fun calculateDelay(targetHour: Int, targetMinute: Int): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour); set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
            return target.timeInMillis - now.timeInMillis
        }
    }
}
