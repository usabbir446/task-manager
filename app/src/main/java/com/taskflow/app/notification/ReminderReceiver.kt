package com.taskflow.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taskflow.app.service.AlarmService

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val taskId = intent?.getLongExtra(EXTRA_TASK_ID, -1L) ?: -1L
        val title = intent?.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"

        if (taskId != -1L) {
            // Trigger loud foreground Alarm service with sound, vibration, and fullscreen dialog
            AlarmService.startAlarm(context, taskId, title)
        }
    }
}
