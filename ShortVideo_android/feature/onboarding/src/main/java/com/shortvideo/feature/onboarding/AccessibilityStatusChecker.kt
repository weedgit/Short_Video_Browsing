package com.shortvideo.feature.onboarding

import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.shortvideo.core.AccessibilityServiceContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class AccessibilityServiceStatus {
    Enabled,
    Disabled,
    Unknown,
}

@Singleton
class AccessibilityStatusChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val serviceComponent = ComponentName(
        context.packageName,
        AccessibilityServiceContract.SERVICE_CLASS_NAME,
    )

    fun getStatus(): AccessibilityServiceStatus {
        val manager = context.getSystemService(AccessibilityManager::class.java)
            ?: return AccessibilityServiceStatus.Unknown

        if (!manager.isEnabled) return AccessibilityServiceStatus.Disabled

        val isOurServiceEnabled = manager.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK,
        ).any { service ->
            serviceComponent == ComponentName(
                service.resolveInfo.serviceInfo.packageName,
                service.resolveInfo.serviceInfo.name,
            )
        }

        return if (isOurServiceEnabled) {
            AccessibilityServiceStatus.Enabled
        } else {
            AccessibilityServiceStatus.Disabled
        }
    }
}
