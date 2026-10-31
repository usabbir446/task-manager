package com.taskflow.app.ui.components

import androidx.compose.animation.AnimatedVisibility
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

    // Parse subtasks
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleCompletion() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = getPriorityColor(task.priority)
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = if (expanded) 10 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (task.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (task.isCompleted) 0.4f else 0.7f
                            ),
                            maxLines = if (expanded) 10 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Tags row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SuggestionChip(
                            onClick = {},
                            label = { 
                                Text(
                                    text = getPriorityLabel(task.priority), 
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = getPriorityColor(task.priority).copy(alpha = 0.15f),
                                labelColor = getPriorityColor(task.priority)
                            ),
                            border = null,
                            modifier = Modifier.height(24.dp)
                        )
                        
                        SuggestionChip(
                            onClick = {},
                            label = { 
                                Text(
                                    text = task.category, 
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = getCategoryColor(task.category).copy(alpha = 0.15f),
                                labelColor = getCategoryColor(task.category)
                            ),
                            border = null,
                            modifier = Modifier.height(24.dp)
                        )

                        if (task.recurrence != "None") {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("🔁 ${task.recurrence}", style = MaterialTheme.typography.labelSmall) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                border = null,
                                modifier = Modifier.height(24.dp)
                            )
                        }
                        
                        if (task.isFromVoice) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Voice task",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Deadline / Due Date & Reminder Notice
                    if (task.dueDate != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Alarm,
                                contentDescription = "Due reminder",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Reminder: ${formatDate(task.dueDate)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // Action Buttons: Edit and Delete
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit task",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete task",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Expandable Subtasks Checklist
            AnimatedVisibility(visible = subtaskList.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 40.dp, top = 8.dp)
                ) {
                    Text(
                        "Checklist (${subtaskList.count { it.second }}/${subtaskList.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    subtaskList.forEachIndexed { index, (subTitle, isDone) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSubtask(index, !isDone) }
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = isDone,
                                onCheckedChange = { onToggleSubtask(index, it) },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = subTitle,
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
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
    3 -> "🔴 High"
    2 -> "🟡 Med"
    else -> "🟢 Low"
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
