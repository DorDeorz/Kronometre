package com.dordeorz.kronometre.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { System, Light, Dark }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val showCentis: Boolean = true,
    val dimScreen: Boolean = true,
    val dynamicColor: Boolean = true,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[THEME] } ?: ThemeMode.System,
            showCentis = prefs[SHOW_CENTIS] ?: true,
            dimScreen = prefs[DIM_SCREEN] ?: true,
            dynamicColor = prefs[DYNAMIC_COLOR] ?: true,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME] = mode.name }
    }

    suspend fun setShowCentis(value: Boolean) {
        dataStore.edit { it[SHOW_CENTIS] = value }
    }

    suspend fun setDimScreen(value: Boolean) {
        dataStore.edit { it[DIM_SCREEN] = value }
    }

    suspend fun setDynamicColor(value: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR] = value }
    }

    companion object {
        private val THEME = stringPreferencesKey("theme")
        private val SHOW_CENTIS = booleanPreferencesKey("show_centis")
        private val DIM_SCREEN = booleanPreferencesKey("dim_screen")
        private val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")

        @Volatile
        private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext.settingsDataStore).also { instance = it }
            }
    }
}
