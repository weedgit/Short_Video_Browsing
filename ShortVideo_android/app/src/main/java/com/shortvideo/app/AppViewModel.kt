package com.shortvideo.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shortvideo.data.preferences.OnboardingPreferences
import com.shortvideo.domain.model.AppThemeMode
import com.shortvideo.domain.repository.AuthRepository
import com.shortvideo.domain.repository.ThemeRepository
import com.shortvideo.feature.onboarding.AccessibilityServiceStatus
import com.shortvideo.feature.onboarding.AccessibilityStatusChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AppViewModel @Inject constructor(
    private val onboardingPreferences: OnboardingPreferences,
    private val authRepository: AuthRepository,
    private val themeRepository: ThemeRepository,
    private val accessibilityStatusChecker: AccessibilityStatusChecker,
) : ViewModel() {
    private val _gateState = MutableStateFlow(AppGateState())
    val gateState: StateFlow<AppGateState> = _gateState.asStateFlow()

    init {
        viewModelScope.launch {
            onboardingPreferences.accessibilityOnboardingCompleted.collect { completed ->
                updateGate(onboardingCompleted = completed)
            }
        }
        viewModelScope.launch {
            authRepository.isAuthenticated.collect { authenticated ->
                updateGate(isAuthenticated = authenticated)
            }
        }
        viewModelScope.launch {
            themeRepository.themeMode.collect { mode ->
                updateGate(themeMode = mode)
            }
        }
    }

    fun refreshAccessibilityStatus() {
        updateGate(serviceStatus = accessibilityStatusChecker.getStatus())
    }

    private fun updateGate(
        onboardingCompleted: Boolean? = null,
        serviceStatus: AccessibilityServiceStatus? = null,
        isAuthenticated: Boolean? = null,
        themeMode: AppThemeMode? = null,
    ) {
        _gateState.update { current ->
            current.copy(
                isReady = true,
                onboardingCompleted = onboardingCompleted ?: current.onboardingCompleted,
                serviceStatus = serviceStatus ?: current.serviceStatus,
                isAuthenticated = isAuthenticated ?: current.isAuthenticated,
                themeMode = themeMode ?: current.themeMode,
            )
        }
    }
}
