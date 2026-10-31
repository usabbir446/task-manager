package com.taskflow.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskflow.app.ui.viewmodel.SettingsUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onMorningNotificationToggle: (Boolean) -> Unit,
    onEveningNotificationToggle: (Boolean) -> Unit,
    onWeeklyRetroToggle: (Boolean) -> Unit,
    onMorningTimeChange: (Int, Int) -> Unit,
    onEveningTimeChange: (Int, Int) -> Unit,
    onVoiceLanguageChange: (String) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onGeofenceToggle: (Boolean) -> Unit
) {
    var showMorningTimePicker by remember { mutableStateOf(false) }
    var showEveningTimePicker by remember { mutableStateOf(false) }

    if (showMorningTimePicker) {
        TimePickerDialog(
            initialHour = uiState.morningHour,
            initialMinute = uiState.morningMinute,
            onConfirm = { hour, minute ->
                onMorningTimeChange(hour, minute)
                showMorningTimePicker = false
            },
            onDismiss = { showMorningTimePicker = false }
        )
    }

    if (showEveningTimePicker) {
        TimePickerDialog(
            initialHour = uiState.eveningHour,
            initialMinute = uiState.eveningMinute,
            onConfirm = { hour, minute ->
                onEveningTimeChange(hour, minute)
                showEveningTimePicker = false
            },
            onDismiss = { showEveningTimePicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Notification Section
            SettingsSectionHeader(icon = Icons.Outlined.Notifications, title = "Smart Briefings & Notifications")

            SettingsToggleItem(
                title = "Morning Summary",
                subtitle = "Remaining tasks briefing as your workday begins",
                checked = uiState.morningNotificationEnabled,
                onCheckedChange = onMorningNotificationToggle
            )

            if (uiState.morningNotificationEnabled) {
                SettingsClickItem(
                    title = "Morning Time",
                    subtitle = formatTime(uiState.morningHour, uiState.morningMinute),
                    icon = Icons.Outlined.WbSunny,
                    onClick = { showMorningTimePicker = true }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsToggleItem(
                title = "Evening Summary",
                subtitle = "Review tasks completed today before wrapping up",
                checked = uiState.eveningNotificationEnabled,
                onCheckedChange = onEveningNotificationToggle
            )

            if (uiState.eveningNotificationEnabled) {
                SettingsClickItem(
                    title = "Evening Time",
                    subtitle = formatTime(uiState.eveningHour, uiState.eveningMinute),
                    icon = Icons.Outlined.NightsStay,
                    onClick = { showEveningTimePicker = true }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Weekly Retrospective
            SettingsToggleItem(
                title = "Weekly Retrospective",
                subtitle = "Sunday evening digest of weekly tasks & top categories",
                checked = uiState.weeklyRetroEnabled,
                onCheckedChange = onWeeklyRetroToggle
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Geofence / Location Section
            SettingsSectionHeader(icon = Icons.Outlined.Place, title = "Office Location Context")

            SettingsToggleItem(
                title = "Auto-Briefing on Office Arrival",
                subtitle = "Trigger morning task briefing when arriving at office coordinates",
                checked = uiState.geofenceEnabled,
                onCheckedChange = onGeofenceToggle
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Voice Section
            SettingsSectionHeader(icon = Icons.Outlined.Mic, title = "Voice AI & Natural Language")

            SettingsRadioGroup(
                title = "Default Voice Language",
                options = listOf(
                    "en" to "🇺🇸 English (with natural time & subtask parsing)",
                    "bn" to "🇧🇩 বাংলা (সময় ও তালিকা শনাক্তকরণসহ)"
                ),
                selectedOption = uiState.defaultVoiceLanguage,
                onOptionSelected = onVoiceLanguageChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Appearance Section
            SettingsSectionHeader(icon = Icons.Outlined.Palette, title = "Appearance")

            SettingsRadioGroup(
                title = "Theme",
                options = listOf(
                    "system" to "🔄 System Default",
                    "light" to "☀️ Light",
                    "dark" to "🌙 Dark"
                ),
                selectedOption = uiState.themeMode,
                onOptionSelected = onThemeModeChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // About & Updates Section
            SettingsSectionHeader(icon = Icons.Outlined.Info, title = "About & Updates")

            val context = androidx.compose.ui.platform.LocalContext.current
            var isCheckingUpdates by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            SettingsClickItem(
                title = "Check for App Updates",
                subtitle = if (isCheckingUpdates) "Checking GitHub for updates..." else "Tap to check and auto-download latest APK",
                icon = Icons.Outlined.SystemUpdate,
                onClick = {
                    if (!isCheckingUpdates) {
                        isCheckingUpdates = true
                        coroutineScope.launch {
                            val updateInfo = com.taskflow.app.util.AppUpdateManager.checkForUpdates()
                            isCheckingUpdates = false
                            if (updateInfo.downloadUrl != null) {
                                com.taskflow.app.util.AppUpdateManager.startDownloadAndInstall(context, updateInfo.downloadUrl)
                            } else {
                                android.widget.Toast.makeText(context, "You are on the latest version!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )

            SettingsClickItem(
                title = "TaskFlow Pro",
                subtitle = "Version 2.2 • Auto-updates enabled",
                icon = Icons.Outlined.TaskAlt,
                onClick = {}
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon, 
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsClickItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun SettingsRadioGroup(
    title: String,
    options: List<Pair<String, String>>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        options.forEach { (value, label) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selectedOption == value, onClick = { onOptionSelected(value) })
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return String.format("%d:%02d %s", displayHour, minute, amPm)
}
