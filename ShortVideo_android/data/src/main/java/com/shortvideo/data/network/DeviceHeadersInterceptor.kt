package com.shortvideo.data.network

import com.shortvideo.data.preferences.DevicePreferences
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class DeviceHeadersInterceptor @Inject constructor(
    private val devicePreferences: DevicePreferences,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val deviceId = runBlocking { devicePreferences.getDeviceId() }

        val request = chain.request().newBuilder()
            .header("X-Device-Id", deviceId)
            .header("X-Platform", "android")
            .header("X-App-Version", "0.1.0")
            .build()

        return chain.proceed(request)
    }
}
