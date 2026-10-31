package com.taskflow.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val MORNING_NOTIFICATION_ENABLED = booleanPreferencesKey("morning_notification_enabled")
        val EVENING_NOTIFICATION_ENABLED = booleanPreferencesKey("evening_notification_enabled")
        val WEEKLY_RETRO_ENABLED = booleanPreferencesKey("weekly_retro_enabled")
        val MORNING_HOUR = intPreferencesKey("morning_hour")
        val MORNING_MINUTE = intPreferencesKey("morning_minute")
        val EVENING_HOUR = intPreferencesKey("evening_hour")
        val EVENING_MINUTE = intPreferencesKey("evening_minute")
        val DEFAULT_VOICE_LANGUAGE = stringPreferencesKey("default_voice_language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        
        // Location Geofencing
        val GEOFENCE_ENABLED = booleanPreferencesKey("geofence_enabled")
        val OFFICE_LATITUDE = doublePreferencesKey("office_latitude")
        val OFFICE_LONGITUDE = doublePreferencesKey("office_longitude")
        val OFFICE_RADIUS = floatPreferencesKey("office_radius")
    }

    val morningNotificationEnabled: Flow<Boolean> = context.dataStore.data.map { it[MORNING_NOTIFICATION_ENABLED] ?: true }
    val eveningNotificationEnabled: Flow<Boolean> = context.dataStore.data.map { it[EVENING_NOTIFICATION_ENABLED] ?: true }
    val weeklyRetroEnabled: Flow<Boolean> = context.dataStore.data.map { it[WEEKLY_RETRO_ENABLED] ?: true }
    val morningHour: Flow<Int> = context.dataStore.data.map { it[MORNING_HOUR] ?: 9 }
    val morningMinute: Flow<Int> = context.dataStore.data.map { it[MORNING_MINUTE] ?: 0 }
    val eveningHour: Flow<Int> = context.dataStore.data.map { it[EVENING_HOUR] ?: 18 }
    val eveningMinute: Flow<Int> = context.dataStore.data.map { it[EVENING_MINUTE] ?: 0 }
    val defaultVoiceLanguage: Flow<String> = context.dataStore.data.map { it[DEFAULT_VOICE_LANGUAGE] ?: "en" }
    val themeMode: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "system" }

    val geofenceEnabled: Flow<Boolean> = context.dataStore.data.map { it[GEOFENCE_ENABLED] ?: false }
    val officeLatitude: Flow<Double> = context.dataStore.data.map { it[OFFICE_LATITUDE] ?: 23.8103 } // Default Dhaka or center
    val officeLongitude: Flow<Double> = context.dataStore.data.map { it[OFFICE_LONGITUDE] ?: 90.4125 }
    val officeRadius: Flow<Float> = context.dataStore.data.map { it[OFFICE_RADIUS] ?: 200f }

    suspend fun setMorningNotification(enabled: Boolean) { context.dataStore.edit { it[MORNING_NOTIFICATION_ENABLED] = enabled } }
    suspend fun setEveningNotification(enabled: Boolean) { context.dataStore.edit { it[EVENING_NOTIFICATION_ENABLED] = enabled } }
    suspend fun setWeeklyRetro(enabled: Boolean) { context.dataStore.edit { it[WEEKLY_RETRO_ENABLED] = enabled } }
    suspend fun setMorningTime(hour: Int, minute: Int) { context.dataStore.edit { it[MORNING_HOUR] = hour; it[MORNING_MINUTE] = minute } }
    suspend fun setEveningTime(hour: Int, minute: Int) { context.dataStore.edit { it[EVENING_HOUR] = hour; it[EVENING_MINUTE] = minute } }
    suspend fun setDefaultVoiceLanguage(language: String) { context.dataStore.edit { it[DEFAULT_VOICE_LANGUAGE] = language } }
    suspend fun setThemeMode(mode: String) { context.dataStore.edit { it[THEME_MODE] = mode } }

    suspend fun setGeofenceSettings(enabled: Boolean, lat: Double, lng: Double, radius: Float) {
        context.dataStore.edit {
            it[GEOFENCE_ENABLED] = enabled
            it[OFFICE_LATITUDE] = lat
            it[OFFICE_LONGITUDE] = lng
            it[OFFICE_RADIUS] = radius
        }
    }
}
