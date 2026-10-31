package com.taskflow.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: Int = 1, // 1: Low, 2: Medium, 3: High
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val completedAt: Long? = null,
    val isFromVoice: Boolean = false,
    val voiceLanguage: String? = null,
    val subTasks: String = "", // Comma-separated or serialized checklist subtasks: "title:false,title2:true"
    val recurrence: String = "None" // "None", "Daily", "Weekly", "Weekdays"
)
