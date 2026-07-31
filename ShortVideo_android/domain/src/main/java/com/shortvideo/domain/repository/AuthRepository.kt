package com.shortvideo.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isAuthenticated: Flow<Boolean>

    suspend fun login(email: String, password: String): Result<Unit>

    suspend fun register(
        email: String,
        password: String,
        username: String,
        displayName: String,
    ): Result<Unit>

    suspend fun logout()

    suspend fun requestPasswordReset(email: String): Result<String?>

    suspend fun confirmPasswordReset(token: String, newPassword: String): Result<Unit>

    suspend fun deleteAccount(): Result<Unit>
}
