package com.shortvideo.app

import com.shortvideo.domain.model.AppThemeMode
import com.shortvideo.feature.onboarding.AccessibilityServiceStatus

data class AppGateState(
    val isReady: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val serviceStatus: AccessibilityServiceStatus = AccessibilityServiceStatus.Unknown,
    val isAuthenticated: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.NIGHT,
) {
    val shouldShowOnboarding: Boolean
        get() = when {
            !onboardingCompleted -> true
            serviceStatus == AccessibilityServiceStatus.Disabled -> true
            else -> false
        }

    val canEnterApp: Boolean
        get() = onboardingCompleted && !shouldShowOnboarding

    val isDarkTheme: Boolean
        get() = themeMode == AppThemeMode.NIGHT
}
