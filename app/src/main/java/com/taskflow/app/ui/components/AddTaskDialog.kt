package com.taskflow.app.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskflow.app.data.local.TaskEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSaveTask: (TaskEntity) -> Unit,
    initialTask: TaskEntity? = null
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var priority by remember { mutableIntStateOf(initialTask?.priority ?: 2) }
    var category by remember { mutableStateOf(initialTask?.category ?: "General") }
    var recurrence by remember { mutableStateOf(initialTask?.recurrence ?: "None") }
    var subtasksInput by remember { 
        mutableStateOf(
            initialTask?.subTasks?.replace(":false", "")?.replace(":true", "") ?: ""
        ) 
    }
    var dueDate by remember { mutableStateOf(initialTask?.dueDate) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var recurrenceExpanded by remember { mutableStateOf(false) }

    val categories = listOf("General", "Work", "Personal", "Shopping", "Study", "Finance")
    val recurrenceOptions = listOf("None", "Daily", "Weekdays", "Weekly")

    val isEditing = initialTask != null && initialTask.id != 0L

    fun showDateTimePicker() {
        val cal = Calendar.getInstance()
        dueDate?.let { cal.timeInMillis = it }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        cal.set(Calendar.MINUTE, minute)
                        cal.set(Calendar.SECOND, 0)
                        dueDate = cal.timeInMillis
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (isEditing) "Edit Task" else if (initialTask != null) "Confirm Voice Task" else "Add New Task",
                style = MaterialTheme.typography.headlineSmall
            ) 
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Voice / Scheduled Detected Date Banner
                if (dueDate != null) {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Scheduled Reminder Date:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    formatFullDateTime(dueDate!!),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    leadingIcon = { Icon(Icons.Filled.Title, "Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Details (optional)") },
                    leadingIcon = { Icon(Icons.Filled.Description, "Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Subtasks checklist input
                OutlinedTextField(
                    value = subtasksInput,
                    onValueChange = { subtasksInput = it },
                    label = { Text("Checklist Steps (comma-separated)") },
                    placeholder = { Text("e.g. Call client, finalize deck, send email") },
                    leadingIcon = { Icon(Icons.Outlined.Checklist, "Subtasks") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Priority
                Text("Priority Level", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = priority == 1,
                        onClick = { priority = 1 },
                        label = { Text("🟢 Low") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = priority == 2,
                        onClick = { priority = 2 },
                        label = { Text("🟡 Med") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = priority == 3,
                        onClick = { priority = 3 },
                        label = { Text("🔴 High") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Reminder / Due Date Picker Field
                OutlinedCard(
                    onClick = { showDateTimePicker() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Alarm,
                            contentDescription = null,
                            tint = if (dueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reminder & Deadline",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (dueDate != null) formatFullDateTime(dueDate!!) else "Set reminder time & date",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (dueDate != null) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (dueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (dueDate != null) {
                            IconButton(onClick = { dueDate = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear reminder")
                            }
                        } else {
                            Icon(Icons.Outlined.EditCalendar, contentDescription = "Pick date time")
                        }
                    }
                }
                
                // Category
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        leadingIcon = { Icon(Icons.Filled.Category, "Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { category = cat; categoryExpanded = false }
                            )
                        }
                    }
                }

                // Recurrence
                ExposedDropdownMenuBox(
                    expanded = recurrenceExpanded,
                    onExpandedChange = { recurrenceExpanded = it }
                ) {
                    OutlinedTextField(
                        value = recurrence,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Recurring Schedule") },
                        leadingIcon = { Icon(Icons.Outlined.Repeat, "Repeat") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recurrenceExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = recurrenceExpanded,
                        onDismissRequest = { recurrenceExpanded = false }
                    ) {
                        recurrenceOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = { recurrence = opt; recurrenceExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val formattedSubtasks = if (subtasksInput.isNotBlank()) {
                            subtasksInput.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .joinToString(",") { "$it:false" }
                        } else ""

                        val baseEntity = initialTask ?: TaskEntity(title = "")
                        val taskToSave = baseEntity.copy(
                            title = title.trim(),
                            description = description.trim(),
                            priority = priority,
                            category = category,
                            dueDate = dueDate,
                            recurrence = recurrence,
                            subTasks = formattedSubtasks
                        )
                        onSaveTask(taskToSave)
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Icon(if (isEditing) Icons.Filled.Save else Icons.Filled.Check, "Save")
                Spacer(Modifier.width(4.dp))
                Text(if (isEditing) "Update Task" else "Save Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun formatFullDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEE, MMM dd • hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
