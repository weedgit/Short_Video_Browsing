package com.shortvideo.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shortvideo.data.preferences.OnboardingPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccessibilityOnboardingUiState(
    val hasOpenedSettings: Boolean = false,
    val serviceStatus: AccessibilityServiceStatus = AccessibilityServiceStatus.Unknown,
)

@HiltViewModel
class AccessibilityOnboardingViewModel @Inject constructor(
    private val onboardingPreferences: OnboardingPreferences,
    private val accessibilityStatusChecker: AccessibilityStatusChecker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccessibilityOnboardingUiState())
    val uiState: StateFlow<AccessibilityOnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            onboardingPreferences.accessibilityConsentAccepted.collect { consentAccepted ->
                _uiState.update { it.copy(hasOpenedSettings = consentAccepted) }
            }
        }
        refreshStatus()
    }

    fun onSettingsOpened() {
        viewModelScope.launch {
            onboardingPreferences.setAccessibilityConsentAccepted(true)
            _uiState.update { it.copy(hasOpenedSettings = true) }
        }
    }

    fun refreshStatus(onCompleted: (() -> Unit)? = null) {
        val status = accessibilityStatusChecker.getStatus()
        val hasConsent = _uiState.value.hasOpenedSettings
        _uiState.update { it.copy(serviceStatus = status) }

        if (status == AccessibilityServiceStatus.Enabled && hasConsent) {
            viewModelScope.launch {
                completeOnboardingInternal()
                onCompleted?.invoke()
            }
        }
    }

    fun completeOnboarding(onCompleted: () -> Unit) {
        if (_uiState.value.serviceStatus != AccessibilityServiceStatus.Enabled) return

        viewModelScope.launch {
            completeOnboardingInternal()
            onCompleted()
        }
    }

    private suspend fun completeOnboardingInternal() {
        onboardingPreferences.setAccessibilityOnboardingCompleted(true)
    }
}
