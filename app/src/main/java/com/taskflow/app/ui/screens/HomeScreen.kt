package com.taskflow.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.app.data.local.TaskEntity
import com.taskflow.app.ui.components.*
import com.taskflow.app.ui.theme.*
import com.taskflow.app.ui.viewmodel.TaskFilter
import com.taskflow.app.ui.viewmodel.TaskUiState
import com.taskflow.app.util.VoiceToTaskParser
import java.text.SimpleDateFormat
import java.util.*

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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var voiceTaskToConfirm by remember { mutableStateOf<TaskEntity?>(null) }

    // Calendar Selection State (defaults to Today)
    var selectedCalendarDate by remember { mutableStateOf(Calendar.getInstance()) }
    var isCalendarFilterActive by remember { mutableStateOf(false) }

    val keyFormat = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val displayDateFormatter = remember { SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()) }

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

    // Add dialog
    if (showAddDialog) {
        val initialScheduledDate = if (isCalendarFilterActive) {
            val scheduledCal = Calendar.getInstance().apply {
                timeInMillis = selectedCalendarDate.timeInMillis
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
            }
            scheduledCal.timeInMillis
        } else null

        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onSaveTask = { task ->
                onAddTask(task)
                Toast.makeText(context, "Task scheduled! ✅", Toast.LENGTH_SHORT).show()
            },
            initialTask = TaskEntity(title = "", dueDate = initialScheduledDate)
        )
    }

    // Edit dialog
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

    val primaryAccent = if (isDark) AmoledGlowTeal else MaterialTheme.colorScheme.primary
    val titleTextColor = if (isDark) Color.White else Color(0xFF121816)
    val mutedTextColor = if (isDark) AmoledMutedText else Color(0xFF6B7A75)
    val cardChipBorder = if (isDark) Color(0xFF1B2421) else Color(0xFFD6DDD9)
    val cardChipUnselectedBg = if (isDark) Color(0xFF0A0F0D) else Color(0xFFF0F4F2)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                                focusedBorderColor = primaryAccent,
                                unfocusedBorderColor = cardChipBorder,
                                focusedTextColor = titleTextColor,
                                unfocusedTextColor = titleTextColor
                            )
                        )
                    } else {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "TaskFlow",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = titleTextColor,
                                    fontSize = 20.sp
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(primaryAccent)
                                )
                            }
                            Text(
                                "${uiState.pendingTasks.size} pending • ${uiState.completedTasks.size} completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = mutedTextColor
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
                            contentDescription = "Search",
                            tint = titleTextColor
                        )
                    }
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = "Export", tint = titleTextColor)
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF0E1412) else Color(0xFFFFFFFF))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export to CSV", color = titleTextColor) },
                                onClick = {
                                    showExportMenu = false
                                    onExportRequested("csv")
                                },
                                leadingIcon = { Icon(Icons.Outlined.TableChart, null, tint = primaryAccent) }
                            )
                            DropdownMenuItem(
                                text = { Text("Export to Markdown Report", color = titleTextColor) },
                                onClick = {
                                    showExportMenu = false
                                    onExportRequested("md")
                                },
                                leadingIcon = { Icon(Icons.Outlined.Description, null, tint = primaryAccent) }
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = titleTextColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = titleTextColor,
                    actionIconContentColor = titleTextColor
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                VoiceInputButton(
                    isListening = isListening,
                    onClick = { startVoiceInput() }
                )
                
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Filled.Add, "Schedule Task") },
                    text = { Text("Schedule Task", fontWeight = FontWeight.Bold) },
                    containerColor = primaryAccent,
                    contentColor = if (isDark) Color(0xFF00201B) else Color.White,
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Productivity Streak Header Card
            ProductivityCard(
                streakDays = uiState.streakDays,
                completedToday = uiState.completedTodayCount,
                mostProductiveWindow = uiState.peakProductivityWindow
            )

            // 2. Interactive Calendar (Full Month View with month navigation)
            ModernCalendarHeader(
                selectedDate = selectedCalendarDate,
                onDateSelected = { newDate ->
                    selectedCalendarDate = newDate
                    isCalendarFilterActive = true
                },
                tasks = uiState.allTasks
            )

            // 3. Filter tabs & Active Date Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Filter Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskFilter.entries.forEach { filter ->
                        val isSelected = uiState.selectedFilter == filter && !isCalendarFilterActive
                        Surface(
                            onClick = {
                                isCalendarFilterActive = false
                                onFilterChange(filter)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                if (isDark) AmoledGlowTeal.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primaryContainer
                            } else {
                                cardChipUnselectedBg
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) primaryAccent else cardChipBorder
                            )
                        ) {
                            Text(
                                text = filter.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) {
                                    if (isDark) AmoledGlowTeal else MaterialTheme.colorScheme.primary
                                } else {
                                    mutedTextColor
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Calendar Active Indicator / Clear Button
                if (isCalendarFilterActive) {
                    Surface(
                        onClick = { isCalendarFilterActive = false },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF182622) else MaterialTheme.colorScheme.primaryContainer,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) AmoledGlowTeal.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "📅 ${displayDateFormatter.format(selectedCalendarDate.time)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryAccent
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Clear date filter",
                                tint = primaryAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 4. Tasks List at the Bottom
            val baseList = when (uiState.selectedFilter) {
                TaskFilter.ALL -> uiState.allTasks
                TaskFilter.PENDING -> uiState.pendingTasks
                TaskFilter.COMPLETED -> uiState.completedTasks
            }

            val displayTasks = if (isCalendarFilterActive) {
                val selectedKey = keyFormat.format(selectedCalendarDate.time)
                val cal = Calendar.getInstance()
                baseList.filter { task ->
                    val stamp = task.dueDate ?: task.createdAt
                    cal.timeInMillis = stamp
                    keyFormat.format(cal.time) == selectedKey
                }
            } else {
                baseList
            }

            if (displayTasks.isEmpty() && !uiState.isLoading) {
                EmptyStateView(
                    title = if (isCalendarFilterActive) "No tasks on this date" else when (uiState.selectedFilter) {
                        TaskFilter.ALL -> "No tasks yet"
                        TaskFilter.PENDING -> "All caught up! 🎉"
                        TaskFilter.COMPLETED -> "Nothing completed yet"
                    },
                    subtitle = if (isCalendarFilterActive)
                        "Tap '+ Schedule Task' to plan a task for ${displayDateFormatter.format(selectedCalendarDate.time)}"
                    else "Speak or tap + to plan your schedule"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp, top = 2.dp)
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
                }
            }
        }
    }
}
