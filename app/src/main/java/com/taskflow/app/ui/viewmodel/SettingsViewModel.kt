package com.taskflow.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taskflow.app.data.preferences.SettingsDataStore
import com.taskflow.app.location.GeofenceManager
import com.taskflow.app.notification.TaskSummaryWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val morningNotificationEnabled: Boolean = true,
    val eveningNotificationEnabled: Boolean = true,
    val weeklyRetroEnabled: Boolean = true,
    val morningHour: Int = 9,
    val morningMinute: Int = 0,
    val eveningHour: Int = 18,
    val eveningMinute: Int = 0,
    val defaultVoiceLanguage: String = "en",
    val themeMode: String = "system",
    val geofenceEnabled: Boolean = false,
    val officeLatitude: Double = 23.8103,
    val officeLongitude: Double = 90.4125,
    val officeRadius: Float = 200f
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val settingsDataStore = SettingsDataStore(application)
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsDataStore.morningNotificationEnabled,
                settingsDataStore.eveningNotificationEnabled,
                settingsDataStore.weeklyRetroEnabled,
                settingsDataStore.morningHour,
                settingsDataStore.morningMinute
            ) { morningEnabled, eveningEnabled, weeklyRetro, morningHour, morningMinute ->
                SettingsBlock1(morningEnabled, eveningEnabled, weeklyRetro, morningHour, morningMinute)
            }.combine(
                combine(
                    settingsDataStore.eveningHour,
                    settingsDataStore.eveningMinute,
                    settingsDataStore.defaultVoiceLanguage,
                    settingsDataStore.themeMode,
                    settingsDataStore.geofenceEnabled
                ) { eveningHour, eveningMinute, voiceLang, theme, geofence ->
                    SettingsBlock2(eveningHour, eveningMinute, voiceLang, theme, geofence)
                }
            ) { b1, b2 ->
                SettingsUiState(
                    morningNotificationEnabled = b1.morningEnabled,
                    eveningNotificationEnabled = b1.eveningEnabled,
                    weeklyRetroEnabled = b1.weeklyRetro,
                    morningHour = b1.morningHour,
                    morningMinute = b1.morningMinute,
                    eveningHour = b2.eveningHour,
                    eveningMinute = b2.eveningMinute,
                    defaultVoiceLanguage = b2.voiceLang,
                    themeMode = b2.theme,
                    geofenceEnabled = b2.geofence
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    private data class SettingsBlock1(
        val morningEnabled: Boolean, val eveningEnabled: Boolean,
        val weeklyRetro: Boolean, val morningHour: Int, val morningMinute: Int
    )

    private data class SettingsBlock2(
        val eveningHour: Int, val eveningMinute: Int,
        val voiceLang: String, val theme: String, val geofence: Boolean
    )

    fun setMorningNotification(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setMorningNotification(enabled)
            val context = getApplication<Application>()
            if (enabled) {
                val state = _uiState.value
                TaskSummaryWorker.scheduleMorningNotification(context, state.morningHour, state.morningMinute)
            } else TaskSummaryWorker.cancelMorningNotification(context)
        }
    }

    fun setEveningNotification(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setEveningNotification(enabled)
            val context = getApplication<Application>()
            if (enabled) {
                val state = _uiState.value
                TaskSummaryWorker.scheduleEveningNotification(context, state.eveningHour, state.eveningMinute)
            } else TaskSummaryWorker.cancelEveningNotification(context)
        }
    }

    fun setWeeklyRetro(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setWeeklyRetro(enabled)
            val context = getApplication<Application>()
            if (enabled) {
                TaskSummaryWorker.scheduleWeeklyRetro(context)
            } else {
                TaskSummaryWorker.cancelWeeklyRetro(context)
            }
        }
    }

    fun setMorningTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsDataStore.setMorningTime(hour, minute)
            if (_uiState.value.morningNotificationEnabled) TaskSummaryWorker.scheduleMorningNotification(getApplication(), hour, minute)
        }
    }

    fun setEveningTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsDataStore.setEveningTime(hour, minute)
            if (_uiState.value.eveningNotificationEnabled) TaskSummaryWorker.scheduleEveningNotification(getApplication(), hour, minute)
        }
    }

    fun setDefaultVoiceLanguage(language: String) { viewModelScope.launch { settingsDataStore.setDefaultVoiceLanguage(language) } }
    fun setThemeMode(mode: String) { viewModelScope.launch { settingsDataStore.setThemeMode(mode) } }

    fun setGeofenceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            settingsDataStore.setGeofenceSettings(enabled, state.officeLatitude, state.officeLongitude, state.officeRadius)
            val context = getApplication<Application>()
            if (enabled) {
                GeofenceManager.setupOfficeGeofence(context, state.officeLatitude, state.officeLongitude, state.officeRadius)
            } else {
                GeofenceManager.removeOfficeGeofence(context)
            }
        }
    }
}
