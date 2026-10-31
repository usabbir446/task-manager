package com.taskflow.app

import android.app.Application
import com.taskflow.app.notification.NotificationHelper

class TaskFlowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }
}
