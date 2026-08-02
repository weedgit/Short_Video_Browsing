package com.shortvideo.data.repository

import com.shortvideo.data.preferences.ThemePreferences
import com.shortvideo.domain.model.AppThemeMode
import com.shortvideo.domain.repository.ThemeRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ThemeRepositoryImpl @Inject constructor(
    private val themePreferences: ThemePreferences,
) : ThemeRepository {
    override val themeMode: Flow<AppThemeMode> =
        themePreferences.themeMode.map { AppThemeMode.fromStorage(it) }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        themePreferences.setThemeMode(mode.storageValue)
    }
}
