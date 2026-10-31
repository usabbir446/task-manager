package com.taskflow.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.taskflow.app.data.local.TaskEntity
import com.taskflow.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskCard(
    task: TaskEntity,
    onToggleCompletion: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleSubtask: (Int, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete \"${task.title}\"?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    val subtaskList = remember(task.subTasks) {
        if (task.subTasks.isBlank()) emptyList()
        else {
            task.subTasks.split(",").mapNotNull { part ->
                val tokens = part.split(":")
                if (tokens.isNotEmpty()) {
                    Pair(tokens[0], tokens.getOrNull(1)?.toBooleanStrictOrNull() ?: false)
                } else null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (task.isCompleted)
                    Brush.verticalGradient(listOf(Color(0xFF080B0A), Color(0xFF040605)))
                else
                    Brush.verticalGradient(listOf(Color(0xFF0E1412), Color(0xFF080C0B)))
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    if (task.isCompleted)
                        listOf(Color(0xFF161E1C), Color(0xFF101614))
                    else
                        listOf(
                            getPriorityColor(task.priority).copy(alpha = 0.35f),
                            Color(0xFF1A2622)
                        )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable { expanded = !expanded }
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleCompletion() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AmoledGlowTeal,
                        checkmarkColor = Color(0xFF00201B),
                        uncheckedColor = getPriorityColor(task.priority)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (task.isCompleted) AmoledMutedText.copy(alpha = 0.6f) else Color.White,
                        maxLines = if (expanded) 10 else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (task.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmoledMutedText.copy(alpha = if (task.isCompleted) 0.4f else 0.85f),
                            maxLines = if (expanded) 10 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Minimal rounded chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = getPriorityColor(task.priority).copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, getPriorityColor(task.priority).copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = getPriorityLabel(task.priority),
                                style = MaterialTheme.typography.labelSmall,
                                color = getPriorityColor(task.priority),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = getCategoryColor(task.category).copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, getCategoryColor(task.category).copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = task.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = getCategoryColor(task.category),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (task.recurrence != "None") {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E2825),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF2E3D39))
                            ) {
                                Text(
                                    text = "🔁 ${task.recurrence}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCCE8E0),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (task.isFromVoice) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Voice task",
                                modifier = Modifier.size(16.dp),
                                tint = AmoledGlowTeal.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Deadline / Due Date & Subtle Glow Reminder Badge
                    if (task.dueDate != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Alarm,
                                contentDescription = "Due reminder",
                                modifier = Modifier.size(14.dp),
                                tint = AmoledGlowTeal
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Reminder: ${formatDate(task.dueDate)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = AmoledGlowTeal
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit task",
                            modifier = Modifier.size(18.dp),
                            tint = AmoledGlowTeal
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete task",
                            modifier = Modifier.size(18.dp),
                            tint = AmoledMutedText.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = subtaskList.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 40.dp, top = 10.dp)
                ) {
                    Text(
                        "Checklist (${subtaskList.count { it.second }}/${subtaskList.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AmoledGlowTeal
                    )
                    subtaskList.forEachIndexed { index, (subTitle, isDone) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSubtask(index, !isDone) }
                                .padding(vertical = 3.dp)
                        ) {
                            Checkbox(
                                checked = isDone,
                                onCheckedChange = { onToggleSubtask(index, it) },
                                modifier = Modifier.size(24.dp),
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AmoledGlowTeal,
                                    checkmarkColor = Color(0xFF00201B)
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = subTitle,
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (isDone) AmoledMutedText.copy(alpha = 0.5f) else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getPriorityColor(priority: Int) = when (priority) {
    3 -> PriorityHigh
    2 -> PriorityMedium
    else -> PriorityLow
}

fun getPriorityLabel(priority: Int) = when (priority) {
    3 -> "High"
    2 -> "Medium"
    else -> "Low"
}

fun getCategoryColor(category: String) = when (category) {
    "Work" -> CategoryWork
    "Personal" -> CategoryPersonal
    "Shopping" -> CategoryShopping
    "Study" -> CategoryStudy
    "Finance" -> CategoryFinance
    else -> CategoryGeneral
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEE, MMM dd • hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
