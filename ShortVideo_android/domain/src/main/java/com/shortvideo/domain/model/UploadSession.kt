package com.shortvideo.domain.model

data class UploadSession(
    val uploadId: String,
    val videoId: String,
    val uploadUrl: String,
    val uploadToken: String,
    val uploadUrlExpiresAt: String,
    val localUri: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val bytesUploaded: Long = 0,
    val status: UploadStatus = UploadStatus.DRAFT,
    val description: String = "",
    val hashtags: List<String> = emptyList(),
    val category: String? = null,
    val errorMessage: String? = null,
)

data class VideoFileInfo(
    val uri: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
)

data class CreateUploadRequest(
    val mimeType: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
)

data class PublishVideoRequest(
    val description: String,
    val hashtags: List<String>,
    val category: String?,
)
