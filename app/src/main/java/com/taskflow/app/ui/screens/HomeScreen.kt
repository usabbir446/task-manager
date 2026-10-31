package com.taskflow.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskflow.app.data.local.TaskEntity
import com.taskflow.app.ui.components.*
import com.taskflow.app.ui.viewmodel.TaskFilter
import com.taskflow.app.ui.viewmodel.TaskUiState
import com.taskflow.app.util.VoiceToTaskParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: TaskUiState,
    onAddTask: (TaskEntity) -> Unit,
    onUpdateTask: (TaskEntity) -> Unit,
    onToggleCompletion: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onToggleSubtask: (TaskEntity, Int, Boolean) -> Unit,
    onFilterChange: (TaskFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    defaultVoiceLanguage: String,
    onExportRequested: (format: String) -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var voiceTaskToConfirm by remember { mutableStateOf<TaskEntity?>(null) }

    // Voice recognition launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                val language = if (defaultVoiceLanguage == "bn") "bn" else "en"
                val task = VoiceToTaskParser.parseVoiceInput(spokenText, language)
                voiceTaskToConfirm = task
            }
        }
    }

    fun startVoiceInput() {
        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (defaultVoiceLanguage == "bn") "bn-BD" else "en-US")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, if (defaultVoiceLanguage == "bn") "bn-BD" else "en-US")
            putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("en-US", "bn-BD"))
            putExtra(RecognizerIntent.EXTRA_PROMPT, if (defaultVoiceLanguage == "bn") "আপনার কাজের কথা বলুন (সময়সহ)..." else "Speak your task with time/steps...")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: Exception) {
            isListening = false
            Toast.makeText(context, "Voice input not available", Toast.LENGTH_SHORT).show()
        }
    }

    // Voice task confirmation dialog
    voiceTaskToConfirm?.let { task ->
        AddTaskDialog(
            onDismiss = { voiceTaskToConfirm = null },
            onSaveTask = { confirmedTask ->
                onAddTask(confirmedTask.copy(isFromVoice = true, voiceLanguage = defaultVoiceLanguage))
                voiceTaskToConfirm = null
                val reminderMsg = if (confirmedTask.dueDate != null) " with alarm reminder!" else "!"
                Toast.makeText(context, "Task saved$reminderMsg ✅", Toast.LENGTH_SHORT).show()
            },
            initialTask = task
        )
    }

    // Manual Add task dialog
    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onSaveTask = { task ->
                onAddTask(task)
                Toast.makeText(context, "Task created! ✅", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Edit task dialog
    taskToEdit?.let { task ->
        AddTaskDialog(
            onDismiss = { taskToEdit = null },
            onSaveTask = { updated ->
                onUpdateTask(updated)
                taskToEdit = null
                Toast.makeText(context, "Task updated! ✏️", Toast.LENGTH_SHORT).show()
            },
            initialTask = task
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search tasks...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                    } else {
                        Column {
                            Text("TaskFlow", fontWeight = FontWeight.Bold)
                            Text(
                                "${uiState.pendingTasks.size} pending • ${uiState.completedTasks.size} completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch) onSearchQueryChange("")
                    }) {
                        Icon(
                            if (showSearch) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = "Search"
                        )
                    }
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = "Export")
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export to CSV") },
                                onClick = {
                                    showExportMenu = false
                                    onExportRequested("csv")
                                },
                                leadingIcon = { Icon(Icons.Outlined.TableChart, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Export to Markdown Report") },
                                onClick = {
                                    showExportMenu = false
                                    onExportRequested("md")
                                },
                                leadingIcon = { Icon(Icons.Outlined.Description, null) }
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VoiceInputButton(
                    isListening = isListening,
                    onClick = { startVoiceInput() }
                )
                
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Filled.Add, "Add Task") },
                    text = { Text("Add Task") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ProductivityCard(
                streakDays = uiState.streakDays,
                completedToday = uiState.completedTodayCount,
                mostProductiveWindow = uiState.peakProductivityWindow
            )

            // Filter tabs
            ScrollableTabRow(
                selectedTabIndex = TaskFilter.entries.indexOf(uiState.selectedFilter),
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                divider = {}
            ) {
                TaskFilter.entries.forEach { filter ->
                    val count = when (filter) {
                        TaskFilter.ALL -> uiState.allTasks.size
                        TaskFilter.PENDING -> uiState.pendingTasks.size
                        TaskFilter.COMPLETED -> uiState.completedTasks.size
                    }
                    Tab(
                        selected = uiState.selectedFilter == filter,
                        onClick = { onFilterChange(filter) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(filter.name.lowercase().replaceFirstChar { it.uppercase() })
                                Badge(
                                    containerColor = if (uiState.selectedFilter == filter) 
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        "$count",
                                        color = if (uiState.selectedFilter == filter) 
                                            MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val displayTasks = when (uiState.selectedFilter) {
                TaskFilter.ALL -> uiState.allTasks
                TaskFilter.PENDING -> uiState.pendingTasks
                TaskFilter.COMPLETED -> uiState.completedTasks
            }

            if (displayTasks.isEmpty() && !uiState.isLoading) {
                EmptyStateView(
                    title = when (uiState.selectedFilter) {
                        TaskFilter.ALL -> "No tasks yet"
                        TaskFilter.PENDING -> "All caught up! 🎉"
                        TaskFilter.COMPLETED -> "Nothing completed yet"
                    },
                    subtitle = when (uiState.selectedFilter) {
                        TaskFilter.ALL -> "Speak or tap + to add your first task with deadlines"
                        TaskFilter.PENDING -> "All pending tasks done. Great work!"
                        TaskFilter.COMPLETED -> "Finished tasks will appear here"
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(
                        items = displayTasks,
                        key = { it.id }
                    ) { task ->
                        TaskCard(
                            task = task,
                            onToggleCompletion = { onToggleCompletion(task) },
                            onEdit = { taskToEdit = task },
                            onDelete = { onDeleteTask(task) },
                            onToggleSubtask = { index, isDone ->
                                onToggleSubtask(task, index, isDone)
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(160.dp))
                    }
                }
            }
        }
    }
}
