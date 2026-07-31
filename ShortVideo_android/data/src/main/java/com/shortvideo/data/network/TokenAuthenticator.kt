package com.shortvideo.data.network

import com.shortvideo.data.preferences.AuthTokenPreferences
import com.shortvideo.data.remote.AuthApi
import com.shortvideo.data.remote.dto.RefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Named

class TokenAuthenticator @Inject constructor(
    private val authTokenPreferences: AuthTokenPreferences,
    @Named("plainAuthApi") private val plainAuthApi: AuthApi,
) : Authenticator {
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) {
            return null
        }

        synchronized(refreshLock) {
            val refreshToken = authTokenPreferences.getRefreshToken() ?: return null

            val newAccessToken = runBlocking {
                runCatching {
                    val envelope = plainAuthApi.refresh(RefreshRequest(refreshToken))
                    val session = envelope.data ?: return@runBlocking null
                    authTokenPreferences.saveSession(
                        accessToken = session.tokens.accessToken,
                        refreshToken = session.tokens.refreshToken,
                    )
                    session.tokens.accessToken
                }.getOrElse {
                    authTokenPreferences.clearSession()
                    null
                }
            } ?: return null

            return response.request.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            result++
            priorResponse = priorResponse.priorResponse
        }
        return result
    }
}
