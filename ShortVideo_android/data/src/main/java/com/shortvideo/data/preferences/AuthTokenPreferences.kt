package com.shortvideo.data.preferences

import com.shortvideo.data.local.dao.AuthTokenDao
import com.shortvideo.data.local.entity.AuthTokenEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthTokenPreferences @Inject constructor(
    private val authTokenDao: AuthTokenDao,
) {
    private val accessTokenCache = AtomicReference<String?>(null)
    private val refreshTokenCache = AtomicReference<String?>(null)

    val isAuthenticated: Flow<Boolean> = authTokenDao.observeToken().map { entity ->
        !entity?.refreshToken.isNullOrBlank()
    }

    suspend fun saveSession(accessToken: String, refreshToken: String) {
        authTokenDao.upsertToken(
            AuthTokenEntity(
                accessToken = accessToken,
                refreshToken = refreshToken,
            ),
        )
        accessTokenCache.set(accessToken)
        refreshTokenCache.set(refreshToken)
    }

    suspend fun clearSession() {
        authTokenDao.clearAll()
        accessTokenCache.set(null)
        refreshTokenCache.set(null)
    }

    fun getAccessToken(): String? {
        accessTokenCache.get()?.let { return it }
        return runBlocking {
            authTokenDao.getToken()?.accessToken
        }.also { accessTokenCache.set(it) }
    }

    fun getRefreshToken(): String? {
        refreshTokenCache.get()?.let { return it }
        return runBlocking {
            authTokenDao.getToken()?.refreshToken
        }.also { refreshTokenCache.set(it) }
    }
}
