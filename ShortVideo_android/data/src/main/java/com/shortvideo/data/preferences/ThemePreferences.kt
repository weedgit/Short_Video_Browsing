package com.shortvideo.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences",
)

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.themeDataStore

    /** `night` or `light`. Defaults to night. */
    val themeMode: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: MODE_NIGHT
    }

    suspend fun setThemeMode(mode: String) {
        val normalized = when (mode.lowercase()) {
            MODE_LIGHT -> MODE_LIGHT
            else -> MODE_NIGHT
        }
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = normalized
        }
    }

    companion object {
        const val MODE_NIGHT = "night"
        const val MODE_LIGHT = "light"
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
