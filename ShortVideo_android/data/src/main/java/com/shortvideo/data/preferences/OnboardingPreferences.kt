package com.shortvideo.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "onboarding_preferences",
)

@Singleton
class OnboardingPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.onboardingDataStore

    val accessibilityOnboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ACCESSIBILITY_ONBOARDING_COMPLETED] ?: false
    }

    val accessibilityConsentAccepted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ACCESSIBILITY_CONSENT_ACCEPTED] ?: false
    }

    suspend fun setAccessibilityOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESSIBILITY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setAccessibilityConsentAccepted(accepted: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESSIBILITY_CONSENT_ACCEPTED] = accepted
        }
    }

    private companion object {
        val KEY_ACCESSIBILITY_ONBOARDING_COMPLETED =
            booleanPreferencesKey("accessibility_onboarding_completed")
        val KEY_ACCESSIBILITY_CONSENT_ACCEPTED =
            booleanPreferencesKey("accessibility_consent_accepted")
    }
}
