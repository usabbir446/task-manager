package com.taskflow.app.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.taskflow.app.data.local.AppDatabase
import com.taskflow.app.data.repository.TaskRepository
import com.taskflow.app.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val event = GeofencingEvent.fromIntent(intent ?: return)
        if (event == null || event.hasError()) return

        if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(context)
                val repo = TaskRepository(db.taskDao())
                val pending = repo.getPendingTasksList()
                val yesterday = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                }.timeInMillis
                val completedYesterday = repo.getCompletedTaskCountSince(yesterday)
                val topTask = pending.firstOrNull()?.let { Pair(it.id, it.title) }

                NotificationHelper.showMorningSummary(
                    context,
                    pending.size,
                    completedYesterday,
                    topTask
                )
            }
        }
    }
}
