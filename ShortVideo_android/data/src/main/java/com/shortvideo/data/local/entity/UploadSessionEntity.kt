package com.shortvideo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upload_sessions")
data class UploadSessionEntity(
    @PrimaryKey val uploadId: String,
    val videoId: String,
    val uploadUrl: String,
    val uploadToken: String,
    val uploadUrlExpiresAt: String,
    val localUri: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val bytesUploaded: Long,
    val status: String,
    val description: String,
    val hashtagsJson: String,
    val category: String?,
    val errorMessage: String?,
    val createdAtMs: Long,
)
