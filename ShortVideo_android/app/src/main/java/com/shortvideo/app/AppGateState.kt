package com.shortvideo.app

import com.shortvideo.feature.onboarding.AccessibilityServiceStatus

data class AppGateState(
    val isReady: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val serviceStatus: AccessibilityServiceStatus = AccessibilityServiceStatus.Unknown,
    val isAuthenticated: Boolean = false,
) {
    val shouldShowOnboarding: Boolean
        get() = when {
            !onboardingCompleted -> true
            serviceStatus == AccessibilityServiceStatus.Disabled -> true
            else -> false
        }

    val canEnterApp: Boolean
        get() = onboardingCompleted && !shouldShowOnboarding
}
