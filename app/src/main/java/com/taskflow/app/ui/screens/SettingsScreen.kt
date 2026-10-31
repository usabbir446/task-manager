package com.taskflow.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.app.ui.theme.AmoledGlowTeal
import com.taskflow.app.ui.theme.AmoledMutedText
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Notification Reminders Section
            SettingsSectionHeader(icon = Icons.Outlined.Notifications, title = "Smart Notifications")

            SettingsToggleItem(
                title = "Morning Motivation & Plan",
                subtitle = "Daily task roadmap & weather context at ${formatTime(uiState.morningHour, uiState.morningMinute)}",
                checked = uiState.morningNotificationEnabled,
                onCheckedChange = onMorningNotificationToggle
            )

            if (uiState.morningNotificationEnabled) {
                SettingsClickItem(
                    title = "Morning Notification Time",
                    subtitle = formatTime(uiState.morningHour, uiState.morningMinute),
                    icon = Icons.Outlined.Schedule,
                    onClick = { showMorningTimePicker = true }
                )
            }

            SettingsToggleItem(
                title = "Evening Reflection",
                subtitle = "Check uncompleted tasks & wrap up your day at ${formatTime(uiState.eveningHour, uiState.eveningMinute)}",
                checked = uiState.eveningNotificationEnabled,
                onCheckedChange = onEveningNotificationToggle
            )

            if (uiState.eveningNotificationEnabled) {
                SettingsClickItem(
                    title = "Evening Notification Time",
                    subtitle = formatTime(uiState.eveningHour, uiState.eveningMinute),
                    icon = Icons.Outlined.Schedule,
                    onClick = { showEveningTimePicker = true }
                )
            }

            SettingsToggleItem(
                title = "Sunday Weekly Retrospective",
                subtitle = "Productivity score, streak overview & insights",
                checked = uiState.weeklyRetroEnabled,
                onCheckedChange = onWeeklyRetroToggle
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Location Section
            SettingsSectionHeader(icon = Icons.Outlined.LocationOn, title = "Location-Based Reminders")

            SettingsToggleItem(
                title = "Office Arrival & Departure",
                subtitle = "Get notified when entering or leaving your workplace",
                checked = uiState.geofenceEnabled,
                onCheckedChange = onGeofenceToggle
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Voice & Language Section
            SettingsSectionHeader(icon = Icons.Outlined.Mic, title = "Voice Input Language")

            SettingsRadioGroup(
                title = "Speech Recognition Primary Language",
                options = listOf("en" to "English (US)", "bn" to "বাংলা (Bangla)"),
                selectedOption = uiState.defaultVoiceLanguage,
                onOptionSelected = onVoiceLanguageChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Appearance Section
            SettingsSectionHeader(icon = Icons.Outlined.Palette, title = "Appearance")

            SettingsRadioGroup(
                title = "Theme Preference",
                options = listOf(
                    "system" to "System Default",
                    "light" to "Light Mode",
                    "dark" to "Super AMOLED Dark"
                ),
                selectedOption = uiState.themeMode,
                onOptionSelected = onThemeModeChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // About & Updates Section
            SettingsSectionHeader(icon = Icons.Outlined.Info, title = "About & Updates")

            val context = androidx.compose.ui.platform.LocalContext.current
            var isCheckingUpdates by remember { mutableStateOf(false) }
            var updateAvailableDialog by remember { mutableStateOf<com.taskflow.app.util.AppUpdateManager.UpdateInfo?>(null) }
            var downloadProgress by remember { mutableStateOf<Int?>(null) }
            var isDownloadSuccess by remember { mutableStateOf(false) }
            var upToDateDialog by remember { mutableStateOf(false) }
            var updateErrorDialog by remember { mutableStateOf<String?>(null) }
            val coroutineScope = rememberCoroutineScope()

            // 1. Checking for Updates Modal Animation
            if (isCheckingUpdates) {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {},
                    title = null,
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CheckingUpdateAnimation()
                            Spacer(Modifier.height(18.dp))
                            Text(
                                "Checking for Updates...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Connecting to GitHub repository...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }

            // 2. Download Progress & Success Modal
            if (downloadProgress != null || isDownloadSuccess) {
                AlertDialog(
                    onDismissRequest = {
                        if (isDownloadSuccess) {
                            downloadProgress = null
                            isDownloadSuccess = false
                            updateAvailableDialog = null
                        }
                    },
                    confirmButton = {
                        if (isDownloadSuccess) {
                            Button(onClick = {
                                downloadProgress = null
                                isDownloadSuccess = false
                                updateAvailableDialog = null
                            }) {
                                Text("Done")
                            }
                        }
                    },
                    title = null,
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isDownloadSuccess) {
                                UpdateSuccessAnimation()
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Download Complete! 🎉",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34C759)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Launching installer... Tap 'Update' on the system prompt to finish.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.CloudDownload,
                                    contentDescription = null,
                                    tint = AmoledGlowTeal,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    "Downloading Update...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(14.dp))

                                // Dynamic Linear Progress Bar
                                LinearProgressIndicator(
                                    progress = { (downloadProgress ?: 0) / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = AmoledGlowTeal,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "TaskFlow v${updateAvailableDialog?.latestVersionName ?: "New"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${downloadProgress ?: 0}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AmoledGlowTeal
                                    )
                                }
                            }
                        }
                    }
                )
            }

            // 3. Update Available Modal
            updateAvailableDialog?.let { update ->
                if (downloadProgress == null && !isDownloadSuccess) {
                    AlertDialog(
                        onDismissRequest = { updateAvailableDialog = null },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.NewReleases,
                                    contentDescription = null,
                                    tint = AmoledGlowTeal,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("New Version Available: v${update.latestVersionName}")
                            }
                        },
                        text = {
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "What's New:",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            update.releaseNotes ?: "Bug fixes and improvements",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("Do you want to download and install this update now?")
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                val url = update.downloadUrl ?: return@Button
                                downloadProgress = 0
                                coroutineScope.launch {
                                    val success = com.taskflow.app.util.AppUpdateManager.downloadAndInstallApk(
                                        context,
                                        url,
                                        onProgress = { downloadProgress = it }
                                    )
                                    if (success) {
                                        isDownloadSuccess = true
                                    } else {
                                        downloadProgress = null
                                    }
                                }
                            }) {
                                Text("Download & Install")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { updateAvailableDialog = null }) {
                                Text("Later")
                            }
                        }
                    )
                }
            }

            // 4. Up-to-Date Modal
            if (upToDateDialog) {
                AlertDialog(
                    onDismissRequest = { upToDateDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("You're Up to Date!")
                        }
                    },
                    text = {
                        Text("TaskFlow v${com.taskflow.app.util.AppUpdateManager.CURRENT_VERSION_NAME} is running the latest build with full calendar and Bangla voice fixes.")
                    },
                    confirmButton = {
                        Button(onClick = { upToDateDialog = false }) {
                            Text("Done")
                        }
                    }
                )
            }

            // 5. Error Dialog (e.g. Private repository)
            updateErrorDialog?.let { errorMsg ->
                AlertDialog(
                    onDismissRequest = { updateErrorDialog = null },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Update Check Failed")
                        }
                    },
                    text = {
                        Text(errorMsg)
                    },
                    confirmButton = {
                        Button(onClick = { updateErrorDialog = null }) {
                            Text("OK")
                        }
                    }
                )
            }

            SettingsClickItem(
                title = "Check for App Updates",
                subtitle = "Tap to check, download and auto-install latest APK",
                icon = Icons.Outlined.SystemUpdate,
                onClick = {
                    if (!isCheckingUpdates && downloadProgress == null) {
                        isCheckingUpdates = true
                        coroutineScope.launch {
                            // Minimum duration to show cool scanning animation
                            val startTime = System.currentTimeMillis()
                            val updateInfo = com.taskflow.app.util.AppUpdateManager.checkForUpdates()
                            val elapsed = System.currentTimeMillis() - startTime
                            if (elapsed < 1200) {
                                kotlinx.coroutines.delay(1200 - elapsed)
                            }
                            isCheckingUpdates = false

                            if (updateInfo.isUpdateAvailable && updateInfo.downloadUrl != null) {
                                updateAvailableDialog = updateInfo
                            } else if (updateInfo.errorMessage != null) {
                                updateErrorDialog = updateInfo.errorMessage
                            } else {
                                upToDateDialog = true
                            }
                        }
                    }
                }
            )

            SettingsClickItem(
                title = "TaskFlow Pro",
                subtitle = "Version ${com.taskflow.app.util.AppUpdateManager.CURRENT_VERSION_NAME} • Auto-updates enabled",
                icon = Icons.Outlined.TaskAlt,
                onClick = {}
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CheckingUpdateAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "checking")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing glow circle
        Box(
            modifier = Modifier
                .size(90.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(AmoledGlowTeal.copy(alpha = 0.12f))
        )
        // Rotating sweep gradient radar ring
        Box(
            modifier = Modifier
                .size(72.dp)
                .rotate(rotation)
                .border(
                    width = 2.5.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            AmoledGlowTeal.copy(alpha = 0.05f),
                            AmoledGlowTeal.copy(alpha = 0.4f),
                            AmoledGlowTeal
                        )
                    ),
                    shape = CircleShape
                )
        )
        // Center Cloud Sync Icon
        Icon(
            imageVector = Icons.Outlined.CloudSync,
            contentDescription = null,
            tint = AmoledGlowTeal,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
fun UpdateSuccessAnimation(modifier: Modifier = Modifier) {
    var startAnim by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.2f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "success_scale"
    )

    LaunchedEffect(Unit) {
        startAnim = true
    }

    Box(
        modifier = modifier.size(90.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color(0xFF34C759).copy(alpha = 0.18f))
        )
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Success",
            tint = Color(0xFF34C759),
            modifier = Modifier
                .size(54.dp)
                .scale(scale)
        )
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
