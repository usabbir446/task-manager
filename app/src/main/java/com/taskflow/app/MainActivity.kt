package com.taskflow.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.taskflow.app.data.preferences.SettingsDataStore
import com.taskflow.app.notification.TaskSummaryWorker
import com.taskflow.app.ui.navigation.TaskFlowNavGraph
import com.taskflow.app.ui.theme.TaskFlowTheme
import com.taskflow.app.ui.viewmodel.SettingsViewModel
import com.taskflow.app.ui.viewmodel.TaskViewModel
import com.taskflow.app.util.VoiceToTaskParser
import com.taskflow.app.widget.TaskWidgetProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_START_VOICE = "extra_start_voice"
    }

    private var pendingVoiceLaunch = false

    private val voiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
            if (spoken.isNotBlank()) {
                val parsed = VoiceToTaskParser.parseVoiceInput(spoken, "en")
                val vm = androidx.lifecycle.ViewModelProvider(this)[TaskViewModel::class.java]
                vm.addTask(parsed)
                TaskWidgetProvider.updateAllWidgets(this)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestPermissions()
        scheduleDefaultNotifications()

        if (intent?.getBooleanExtra(EXTRA_START_VOICE, false) == true) {
            pendingVoiceLaunch = true
        }

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()
            
            val darkTheme = when (settingsState.themeMode) {
                "dark" -> true
                "light" -> false
                else -> null
            }

            LaunchedEffect(pendingVoiceLaunch) {
                if (pendingVoiceLaunch) {
                    pendingVoiceLaunch = false
                    val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Quick Add Task...")
                    }
                    try {
                        voiceLauncher.launch(voiceIntent)
                    } catch (_: Exception) {}
                }
            }

            TaskFlowTheme(
                darkTheme = darkTheme ?: isSystemInDarkTheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    TaskFlowNavGraph(
                        navController = navController,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (notGranted.isNotEmpty()) {
            requestPermissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun scheduleDefaultNotifications() {
        val settingsDataStore = SettingsDataStore(this)
        kotlinx.coroutines.MainScope().launch {
            val morningEnabled = settingsDataStore.morningNotificationEnabled.first()
            val eveningEnabled = settingsDataStore.eveningNotificationEnabled.first()
            val weeklyRetroEnabled = settingsDataStore.weeklyRetroEnabled.first()
            
            if (morningEnabled) {
                val hour = settingsDataStore.morningHour.first()
                val minute = settingsDataStore.morningMinute.first()
                TaskSummaryWorker.scheduleMorningNotification(this@MainActivity, hour, minute)
            }
            if (eveningEnabled) {
                val hour = settingsDataStore.eveningHour.first()
                val minute = settingsDataStore.eveningMinute.first()
                TaskSummaryWorker.scheduleEveningNotification(this@MainActivity, hour, minute)
            }
            if (weeklyRetroEnabled) {
                TaskSummaryWorker.scheduleWeeklyRetro(this@MainActivity)
            }
        }
    }
}
