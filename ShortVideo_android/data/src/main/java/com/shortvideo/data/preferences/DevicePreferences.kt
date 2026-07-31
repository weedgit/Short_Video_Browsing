package com.shortvideo.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.deviceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "device_preferences",
)

@Singleton
class DevicePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.deviceDataStore

    suspend fun getDeviceId(): String {
        val current = dataStore.data.first()[KEY_DEVICE_ID]
        if (!current.isNullOrBlank()) {
            return current
        }

        val generated = UUID.randomUUID().toString()
        dataStore.edit { prefs ->
            prefs[KEY_DEVICE_ID] = generated
        }
        return generated
    }

    fun observeDeviceId(): kotlinx.coroutines.flow.Flow<String> =
        dataStore.data.map { prefs ->
            prefs[KEY_DEVICE_ID] ?: ""
        }

    private companion object {
        val KEY_DEVICE_ID = stringPreferencesKey("device_id")
    }
}
