package com.taskflow.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taskflow.app.data.preferences.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val settingsDataStore = SettingsDataStore(context)
            CoroutineScope(Dispatchers.IO).launch {
                val morningEnabled = settingsDataStore.morningNotificationEnabled.first()
                val eveningEnabled = settingsDataStore.eveningNotificationEnabled.first()
                val weeklyRetroEnabled = settingsDataStore.weeklyRetroEnabled.first()
                val morningHour = settingsDataStore.morningHour.first()
                val morningMinute = settingsDataStore.morningMinute.first()
                val eveningHour = settingsDataStore.eveningHour.first()
                val eveningMinute = settingsDataStore.eveningMinute.first()

                if (morningEnabled) TaskSummaryWorker.scheduleMorningNotification(context, morningHour, morningMinute)
                if (eveningEnabled) TaskSummaryWorker.scheduleEveningNotification(context, eveningHour, eveningMinute)
                if (weeklyRetroEnabled) TaskSummaryWorker.scheduleWeeklyRetro(context)
            }
        }
    }
}
