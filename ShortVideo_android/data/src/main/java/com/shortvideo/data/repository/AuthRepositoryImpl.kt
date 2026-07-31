package com.shortvideo.data.repository

import com.shortvideo.data.preferences.AuthTokenPreferences
import com.shortvideo.data.remote.ApiErrorParser
import com.shortvideo.data.remote.AuthApi
import com.shortvideo.data.remote.dto.ApiEnvelope
import com.shortvideo.data.remote.dto.AuthSessionDto
import com.shortvideo.data.remote.dto.DeleteAccountRequestDto
import com.shortvideo.data.remote.dto.LoginRequest
import com.shortvideo.data.remote.dto.PasswordResetConfirmRequestDto
import com.shortvideo.data.remote.dto.PasswordResetRequestDto
import com.shortvideo.data.remote.dto.RefreshRequest
import com.shortvideo.data.remote.dto.RegisterRequest
import com.shortvideo.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val authTokenPreferences: AuthTokenPreferences,
) : AuthRepository {
    override val isAuthenticated: Flow<Boolean> = authTokenPreferences.isAuthenticated

    override suspend fun login(email: String, password: String): Result<Unit> =
        executeAuthCall {
            authApi.login(
                LoginRequest(
                    email = email.trim(),
                    password = password,
                ),
            )
        }

    override suspend fun register(
        email: String,
        password: String,
        username: String,
        displayName: String,
    ): Result<Unit> = executeAuthCall {
        authApi.register(
            RegisterRequest(
                email = email.trim(),
                password = password,
                username = username.trim(),
                displayName = displayName.trim(),
            ),
        )
    }

    override suspend fun logout() {
        val refreshToken = authTokenPreferences.getRefreshToken()
        if (!refreshToken.isNullOrBlank()) {
            runCatching {
                authApi.logout(RefreshRequest(refreshToken))
            }
        }
        authTokenPreferences.clearSession()
    }

    override suspend fun requestPasswordReset(email: String): Result<String?> {
        return try {
            val envelope = authApi.requestPasswordReset(PasswordResetRequestDto(email.trim()))
            Result.success(envelope.data?.resetToken)
        } catch (throwable: Throwable) {
            Result.failure(
                IllegalStateException(ApiErrorParser.messageFrom(throwable), throwable),
            )
        }
    }

    override suspend fun confirmPasswordReset(token: String, newPassword: String): Result<Unit> {
        return try {
            authApi.confirmPasswordReset(
                PasswordResetConfirmRequestDto(
                    token = token.trim(),
                    newPassword = newPassword,
                ),
            )
            Result.success(Unit)
        } catch (throwable: Throwable) {
            Result.failure(
                IllegalStateException(ApiErrorParser.messageFrom(throwable), throwable),
            )
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val refreshToken = authTokenPreferences.getRefreshToken()
            authApi.deleteAccount(DeleteAccountRequestDto(refreshToken = refreshToken))
            authTokenPreferences.clearSession()
            Result.success(Unit)
        } catch (throwable: Throwable) {
            Result.failure(
                IllegalStateException(ApiErrorParser.messageFrom(throwable), throwable),
            )
        }
    }

    private suspend fun executeAuthCall(
        call: suspend () -> ApiEnvelope<AuthSessionDto>,
    ): Result<Unit> {
        return try {
            val envelope = call()
            val session = envelope.data
                ?: return Result.failure(IllegalStateException("Empty auth response"))
            authTokenPreferences.saveSession(
                accessToken = session.tokens.accessToken,
                refreshToken = session.tokens.refreshToken,
            )
            Result.success(Unit)
        } catch (throwable: Throwable) {
            Result.failure(
                IllegalStateException(ApiErrorParser.messageFrom(throwable), throwable),
            )
        }
    }
}
