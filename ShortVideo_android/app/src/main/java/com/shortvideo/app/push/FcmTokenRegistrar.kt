package com.shortvideo.app.push

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.shortvideo.domain.repository.InboxRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Registers a device push token with the backend.
 * Uses a stable device-derived token until Firebase Cloud Messaging is configured
 * (add google-services.json + firebase-messaging dependency for production FCM).
 */
@Singleton
class FcmTokenRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val inboxRepository: InboxRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun registerIfNeeded() {
        scope.launch {
            val deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID,
            ) ?: "unknown-device"
            val token = "dev-fcm-$deviceId"
            runCatching {
                inboxRepository.registerFcmToken(deviceId = deviceId, fcmToken = token)
            }.onSuccess {
                Log.i(TAG, "Registered push token with backend")
            }.onFailure { error ->
                Log.w(TAG, "Push token registration skipped: ${error.message}")
            }
        }
    }

    private companion object {
        const val TAG = "FcmTokenRegistrar"
    }
}
