package com.shortvideo.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.feedPlaybackDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "feed_playback_preferences",
)

@Singleton
class FeedPlaybackPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mutedKey = booleanPreferencesKey("feed_muted")
    private fun resumeKey(videoId: String) = longPreferencesKey("resume_$videoId")

    suspend fun isMuted(): Boolean =
        context.feedPlaybackDataStore.data.first()[mutedKey] ?: false

    suspend fun setMuted(muted: Boolean) {
        context.feedPlaybackDataStore.edit { prefs ->
            prefs[mutedKey] = muted
        }
    }

    suspend fun getResumePositionMs(videoId: String): Long =
        context.feedPlaybackDataStore.data.first()[resumeKey(videoId)] ?: 0L

    suspend fun setResumePositionMs(videoId: String, positionMs: Long) {
        context.feedPlaybackDataStore.edit { prefs ->
            prefs[resumeKey(videoId)] = positionMs
        }
    }
}
