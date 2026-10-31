package com.taskflow.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.taskflow.app.MainActivity
import com.taskflow.app.R
import com.taskflow.app.data.local.AppDatabase
import com.taskflow.app.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        if (intent?.action == ACTION_TOGGLE_TASK) {
            val taskId = intent.getLongExtra(EXTRA_WIDGET_TASK_ID, -1L)
            if (taskId != -1L) {
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getDatabase(context)
                    val repo = TaskRepository(db.taskDao())
                    val task = repo.getTaskById(taskId)
                    if (task != null) {
                        repo.toggleTaskCompletion(task)
                        updateAllWidgets(context)
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_TASK = "com.taskflow.app.ACTION_TOGGLE_TASK"
        const val EXTRA_WIDGET_TASK_ID = "extra_widget_task_id"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_task_layout)

            // Open app on header click
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, appPendingIntent)

            // Quick add voice on microphone click
            val voiceIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_START_VOICE, true)
            }
            val voicePendingIntent = PendingIntent.getActivity(
                context, 501, voiceIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_mic_btn, voicePendingIntent)

            // Load pending tasks asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(context)
                val repo = TaskRepository(db.taskDao())
                val pending = repo.getPendingTasksList()

                views.setTextViewText(R.id.widget_pending_count, "${pending.size} pending")

                val firstTask = pending.firstOrNull()
                if (firstTask != null) {
                    views.setTextViewText(R.id.widget_task_title, firstTask.title)
                    views.setTextViewText(R.id.widget_task_category, firstTask.category)

                    val checkIntent = Intent(context, TaskWidgetProvider::class.java).apply {
                        action = ACTION_TOGGLE_TASK
                        putExtra(EXTRA_WIDGET_TASK_ID, firstTask.id)
                    }
                    val checkPendingIntent = PendingIntent.getBroadcast(
                        context, firstTask.id.toInt() + 6000, checkIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_check_btn, checkPendingIntent)
                } else {
                    views.setTextViewText(R.id.widget_task_title, "All tasks completed! 🎉")
                    views.setTextViewText(R.id.widget_task_category, "Relax")
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TaskWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }
}
