package com.shortvideo.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateUploadRequestDto(
    @SerializedName("mimeType") val mimeType: String,
    @SerializedName("fileSizeBytes") val fileSizeBytes: Long,
    @SerializedName("durationMs") val durationMs: Long,
)

data class CreateUploadResponseDto(
    @SerializedName("uploadId") val uploadId: String,
    @SerializedName("videoId") val videoId: String,
    @SerializedName("uploadUrl") val uploadUrl: String,
    @SerializedName("uploadToken") val uploadToken: String,
    @SerializedName("uploadUrlExpiresAt") val uploadUrlExpiresAt: String,
    @SerializedName("status") val status: String,
)

data class UploadProgressRequestDto(
    @SerializedName("bytesUploaded") val bytesUploaded: Long,
)

data class UploadProgressResponseDto(
    @SerializedName("uploadId") val uploadId: String,
    @SerializedName("videoId") val videoId: String,
    @SerializedName("status") val status: String,
    @SerializedName("bytesUploaded") val bytesUploaded: String,
    @SerializedName("fileSizeBytes") val fileSizeBytes: String,
)

data class PublishVideoRequestDto(
    @SerializedName("description") val description: String,
    @SerializedName("hashtags") val hashtags: List<String>,
    @SerializedName("category") val category: String?,
)

data class PublishVideoResponseDto(
    @SerializedName("videoId") val videoId: String,
    @SerializedName("status") val status: String,
)

data class DevUploadCompleteResponseDto(
    @SerializedName("success") val success: Boolean,
)

data class CancelUploadResponseDto(
    @SerializedName("uploadId") val uploadId: String,
    @SerializedName("cancelled") val cancelled: Boolean,
)
