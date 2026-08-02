package com.shortvideo.domain.repository

import com.shortvideo.domain.model.AppThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    val themeMode: Flow<AppThemeMode>
    suspend fun setThemeMode(mode: AppThemeMode)
}
