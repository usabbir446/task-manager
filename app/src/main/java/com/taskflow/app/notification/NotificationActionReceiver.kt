package com.taskflow.app.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taskflow.app.data.local.AppDatabase
import com.taskflow.app.data.repository.TaskRepository
import com.taskflow.app.widget.TaskWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_DONE = "com.taskflow.app.ACTION_MARK_DONE"
        const val ACTION_SNOOZE_1H = "com.taskflow.app.ACTION_SNOOZE_1H"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationId != -1) {
            notificationManager?.cancel(notificationId)
        }

        when (action) {
            ACTION_MARK_DONE -> {
                if (taskId != -1L) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppDatabase.getDatabase(context)
                        val repo = TaskRepository(db.taskDao())
                        val task = repo.getTaskById(taskId)
                        if (task != null && !task.isCompleted) {
                            repo.toggleTaskCompletion(task)
                            TaskWidgetProvider.updateAllWidgets(context)
                        }
                    }
                }
            }
            ACTION_SNOOZE_1H -> {
                // Postponed reminder for 1 hour
            }
        }
    }
}
