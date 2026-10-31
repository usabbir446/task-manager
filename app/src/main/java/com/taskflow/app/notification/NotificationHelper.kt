package com.taskflow.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.taskflow.app.MainActivity
import com.taskflow.app.R

object NotificationHelper {

    const val CHANNEL_ID_TASK_SUMMARY = "task_summary_channel"
    const val CHANNEL_ID_TASK_REMINDER = "task_reminder_channel"
    const val CHANNEL_ID_WEEKLY_RETRO = "weekly_retro_channel"

    private const val MORNING_NOTIFICATION_ID = 1001
    private const val EVENING_NOTIFICATION_ID = 1002
    private const val WEEKLY_RETRO_NOTIFICATION_ID = 1003

    fun createNotificationChannels(context: Context) {
        val summaryChannel = NotificationChannel(
            CHANNEL_ID_TASK_SUMMARY, "Task Summary", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Morning and evening task summary notifications" }

        val reminderChannel = NotificationChannel(
            CHANNEL_ID_TASK_REMINDER, "Task Reminders", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Individual task alerts and deadlines" }

        val weeklyChannel = NotificationChannel(
            CHANNEL_ID_WEEKLY_RETRO, "Weekly Retrospective", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Weekly productivity digest and analytics" }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(summaryChannel)
        notificationManager.createNotificationChannel(reminderChannel)
        notificationManager.createNotificationChannel(weeklyChannel)
    }

    fun showMorningSummary(
        context: Context,
        pendingCount: Int,
        completedYesterday: Int,
        topPendingTask: Pair<Long, String>?
    ) {
        val title = "🌅 Good Morning! Task Summary"
        val firstTaskText = topPendingTask?.second?.let { "\nTop priority: $it" } ?: ""
        val body = "📋 Pending: $pendingCount tasks | ✅ Yesterday: $completedYesterday$firstTaskText"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_TASK_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)

        // Action 1: Mark top task done directly
        if (topPendingTask != null) {
            val markDoneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_MARK_DONE
                putExtra(NotificationActionReceiver.EXTRA_TASK_ID, topPendingTask.first)
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, MORNING_NOTIFICATION_ID)
            }
            val markDonePendingIntent = PendingIntent.getBroadcast(
                context, 101, markDoneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_notification, "Mark Top Done", markDonePendingIntent)
        }

        // Action 2: Snooze 1 Hour
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE_1H
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, MORNING_NOTIFICATION_ID)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, 102, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(R.drawable.ic_notification, "Snooze 1h", snoozePendingIntent)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(MORNING_NOTIFICATION_ID, builder.build())
    }

    fun showEveningSummary(context: Context, completedToday: Int, remainingCount: Int, remainingNames: List<String>) {
        val title = "🌆 Evening Summary - Great work today!"
        val remainingList = if (remainingNames.isNotEmpty()) {
            remainingNames.take(4).joinToString("\n") { "• $it" } +
                    if (remainingNames.size > 4) "\n  ...and ${remainingNames.size - 4} more" else ""
        } else "All tasks completed! 🎉"
        val body = "✅ Completed today: $completedToday tasks\n📋 Still remaining: $remainingCount\n\n$remainingList"

        val openIntent = Intent(context, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TASK_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body.lines().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(EVENING_NOTIFICATION_ID, notification)
    }

    fun showWeeklyRetro(context: Context, completedThisWeek: Int, topCategory: String, pendingCount: Int) {
        val title = "📊 Weekly Productivity Retrospective"
        val body = "You completed $completedThisWeek tasks this week!\n🏆 Top Category: $topCategory\n📋 Leftover for next week: $pendingCount"

        val openIntent = Intent(context, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_WEEKLY_RETRO)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(WEEKLY_RETRO_NOTIFICATION_ID, notification)
    }

    fun showTaskReminder(context: Context, taskId: Long, title: String) {
        val openIntent = Intent(context, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            context, taskId.toInt(), openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markDoneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_DONE
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, taskId.toInt())
        }
        val markDonePendingIntent = PendingIntent.getBroadcast(
            context, taskId.toInt() + 5000, markDoneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TASK_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏰ Task Due: $title")
            .setContentText("It's time to work on this task!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_notification, "Mark Done", markDonePendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(taskId.toInt(), notification)
    }
}
