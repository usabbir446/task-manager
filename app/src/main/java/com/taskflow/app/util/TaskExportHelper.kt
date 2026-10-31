package com.taskflow.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.taskflow.app.data.local.TaskEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object TaskExportHelper {

    fun exportTasksToCsv(context: Context, tasks: List<TaskEntity>): File? {
        val fileName = "TaskFlow_Export_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val sb = StringBuilder()
        sb.append("ID,Title,Description,Status,Priority,Category,Recurrence,Due Date,Created At,Subtasks\n")

        for (task in tasks) {
            val status = if (task.isCompleted) "Completed" else "Pending"
            val priority = when (task.priority) {
                3 -> "High"
                2 -> "Medium"
                else -> "Low"
            }
            val dueDateStr = task.dueDate?.let { dateFormat.format(Date(it)) } ?: ""
            val createdAtStr = dateFormat.format(Date(task.createdAt))
            val escapedTitle = "\"${task.title.replace("\"", "\"\"")}\""
            val escapedDesc = "\"${task.description.replace("\"", "\"\"")}\""
            val escapedSubtasks = "\"${task.subTasks.replace("\"", "\"\"")}\""

            sb.append("${task.id},$escapedTitle,$escapedDesc,$status,$priority,${task.category},${task.recurrence},$dueDateStr,$createdAtStr,$escapedSubtasks\n")
        }

        return try {
            file.writeText(sb.toString())
            file
        } catch (e: Exception) {
            null
        }
    }

    fun exportTasksToMarkdown(context: Context, tasks: List<TaskEntity>): File? {
        val fileName = "TaskFlow_Report_${System.currentTimeMillis()}.md"
        val file = File(context.cacheDir, fileName)

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("# 📋 TaskFlow Productivity Report\n\n")
        sb.append("*Generated on: ${dateFormat.format(Date())}*\n\n")

        val pending = tasks.filter { !it.isCompleted }
        val completed = tasks.filter { it.isCompleted }

        sb.append("## 📌 Remaining Tasks (${pending.size})\n")
        if (pending.isEmpty()) {
            sb.append("No pending tasks. Great job!\n\n")
        } else {
            for (t in pending) {
                val priorityBadge = when (t.priority) {
                    3 -> "🔴 [High]"
                    2 -> "🟡 [Med]"
                    else -> "🟢 [Low]"
                }
                sb.append("- [ ] **${t.title}** $priorityBadge (${t.category})\n")
                if (t.description.isNotEmpty()) {
                    sb.append("  - *${t.description}*\n")
                }
            }
            sb.append("\n")
        }

        sb.append("## ✅ Completed Tasks (${completed.size})\n")
        if (completed.isEmpty()) {
            sb.append("No completed tasks yet.\n")
        } else {
            for (t in completed) {
                sb.append("- [x] ~~${t.title}~~ (${t.category})\n")
            }
        }

        return try {
            file.writeText(sb.toString())
            file
        } catch (e: Exception) {
            null
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Tasks")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
