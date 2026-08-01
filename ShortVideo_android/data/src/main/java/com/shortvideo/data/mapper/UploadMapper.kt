package com.shortvideo.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shortvideo.data.local.entity.UploadSessionEntity
import com.shortvideo.data.remote.dto.CreateUploadResponseDto
import com.shortvideo.domain.model.PublishVideoRequest
import com.shortvideo.domain.model.UploadSession
import com.shortvideo.domain.model.UploadStatus
import com.shortvideo.domain.model.VideoFileInfo

private val gson = Gson()
private val stringListType = object : TypeToken<List<String>>() {}.type

fun CreateUploadResponseDto.toEntity(
    video: VideoFileInfo,
    publish: PublishVideoRequest,
): UploadSessionEntity =
    UploadSessionEntity(
        uploadId = uploadId,
        videoId = videoId,
        uploadUrl = uploadUrl,
        uploadToken = uploadToken,
        uploadUrlExpiresAt = uploadUrlExpiresAt,
        localUri = video.uri,
        mimeType = video.mimeType,
        fileSizeBytes = video.fileSizeBytes,
        durationMs = video.durationMs,
        bytesUploaded = 0,
        status = UploadStatus.DRAFT.name,
        description = publish.description,
        hashtagsJson = gson.toJson(publish.hashtags),
        category = publish.category,
        errorMessage = null,
        createdAtMs = System.currentTimeMillis(),
        provider = provider ?: "alibaba_vod",
        uploadAuth = uploadAuth,
        uploadAddress = uploadAddress,
    )

fun UploadSessionEntity.toDomain(): UploadSession =
    UploadSession(
        uploadId = uploadId,
        videoId = videoId,
        uploadUrl = uploadUrl,
        uploadToken = uploadToken,
        uploadUrlExpiresAt = uploadUrlExpiresAt,
        localUri = localUri,
        mimeType = mimeType,
        fileSizeBytes = fileSizeBytes,
        durationMs = durationMs,
        bytesUploaded = bytesUploaded,
        status = runCatching { UploadStatus.valueOf(status) }.getOrDefault(UploadStatus.FAILED),
        description = description,
        hashtags = gson.fromJson(hashtagsJson, stringListType) ?: emptyList(),
        category = category,
        errorMessage = errorMessage,
        provider = provider,
        uploadAuth = uploadAuth,
        uploadAddress = uploadAddress,
    )

fun UploadSession.toEntity(): UploadSessionEntity =
    UploadSessionEntity(
        uploadId = uploadId,
        videoId = videoId,
        uploadUrl = uploadUrl,
        uploadToken = uploadToken,
        uploadUrlExpiresAt = uploadUrlExpiresAt,
        localUri = localUri,
        mimeType = mimeType,
        fileSizeBytes = fileSizeBytes,
        durationMs = durationMs,
        bytesUploaded = bytesUploaded,
        status = status.name,
        description = description,
        hashtagsJson = gson.toJson(hashtags),
        category = category,
        errorMessage = errorMessage,
        createdAtMs = System.currentTimeMillis(),
        provider = provider,
        uploadAuth = uploadAuth,
        uploadAddress = uploadAddress,
    )
