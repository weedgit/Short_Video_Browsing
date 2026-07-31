package com.shortvideo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auth_tokens")
data class AuthTokenEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val accessToken: String?,
    val refreshToken: String?,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
