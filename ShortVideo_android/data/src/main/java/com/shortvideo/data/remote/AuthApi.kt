package com.shortvideo.data.remote

import com.shortvideo.data.remote.dto.ApiEnvelope
import com.shortvideo.data.remote.dto.AuthSessionDto
import com.shortvideo.data.remote.dto.DeleteAccountRequestDto
import com.shortvideo.data.remote.dto.LoginRequest
import com.shortvideo.data.remote.dto.LogoutResponseDto
import com.shortvideo.data.remote.dto.PasswordResetConfirmRequestDto
import com.shortvideo.data.remote.dto.PasswordResetRequestDto
import com.shortvideo.data.remote.dto.PasswordResetRequestResponseDto
import com.shortvideo.data.remote.dto.RefreshRequest
import com.shortvideo.data.remote.dto.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST

interface AuthApi {
    @POST("v1/auth/login")
    suspend fun login(@Body body: LoginRequest): ApiEnvelope<AuthSessionDto>

    @POST("v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiEnvelope<AuthSessionDto>

    @POST("v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): ApiEnvelope<AuthSessionDto>

    @POST("v1/auth/logout")
    suspend fun logout(@Body body: RefreshRequest): ApiEnvelope<LogoutResponseDto>

    @POST("v1/auth/password/reset/request")
    suspend fun requestPasswordReset(
        @Body body: PasswordResetRequestDto,
    ): ApiEnvelope<PasswordResetRequestResponseDto>

    @POST("v1/auth/password/reset/confirm")
    suspend fun confirmPasswordReset(
        @Body body: PasswordResetConfirmRequestDto,
    ): ApiEnvelope<LogoutResponseDto>

    @HTTP(method = "DELETE", path = "v1/account", hasBody = true)
    suspend fun deleteAccount(@Body body: DeleteAccountRequestDto): ApiEnvelope<LogoutResponseDto>
}
