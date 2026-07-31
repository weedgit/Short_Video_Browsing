package com.shortvideo.data.network

import com.shortvideo.data.preferences.AuthTokenPreferences
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val authTokenPreferences: AuthTokenPreferences,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.encodedPath.contains("/v1/auth/")) {
            return chain.proceed(request)
        }

        val accessToken = authTokenPreferences.getAccessToken()
        if (accessToken.isNullOrBlank()) {
            return chain.proceed(request)
        }

        val authenticatedRequest = request.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
