package com.shortvideo.domain.model

data class AuthUser(
    val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
)
