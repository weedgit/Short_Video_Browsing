package com.shortvideo.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ApiEnvelope<T>(
    @SerializedName("data") val data: T?,
)

data class ApiErrorBody(
    @SerializedName("error") val error: ApiErrorDto?,
)

data class ApiErrorDto(
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
)

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("username") val username: String,
    @SerializedName("displayName") val displayName: String,
)

data class RefreshRequest(
    @SerializedName("refreshToken") val refreshToken: String,
)

data class AuthSessionDto(
    @SerializedName("user") val user: AuthUserDto,
    @SerializedName("tokens") val tokens: AuthTokensDto,
)

data class AuthUserDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("avatarUrl") val avatarUrl: String?,
)

data class AuthTokensDto(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("accessTokenExpiresIn") val accessTokenExpiresIn: Int,
    @SerializedName("tokenType") val tokenType: String,
)

data class LogoutResponseDto(
    @SerializedName("success") val success: Boolean,
)

data class PasswordResetRequestDto(
    @SerializedName("email") val email: String,
)

data class PasswordResetRequestResponseDto(
    @SerializedName("message") val message: String,
    @SerializedName("resetToken") val resetToken: String? = null,
)

data class PasswordResetConfirmRequestDto(
    @SerializedName("token") val token: String,
    @SerializedName("newPassword") val newPassword: String,
)

data class DeleteAccountRequestDto(
    @SerializedName("refreshToken") val refreshToken: String? = null,
)
