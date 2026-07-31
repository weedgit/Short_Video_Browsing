package com.shortvideo.domain.repository

import com.shortvideo.domain.model.CreateUploadRequest
import com.shortvideo.domain.model.PublishVideoRequest
import com.shortvideo.domain.model.UploadSession
import com.shortvideo.domain.model.VideoFileInfo
import kotlinx.coroutines.flow.Flow

interface UploadRepository {
    suspend fun inspectVideo(uri: String): Result<VideoFileInfo>

    suspend fun createUploadSession(
        video: VideoFileInfo,
        publish: PublishVideoRequest,
    ): Result<UploadSession>

    fun observeSession(uploadId: String): Flow<UploadSession?>

    fun observeActiveSession(): Flow<UploadSession?>

    suspend fun getSession(uploadId: String): UploadSession?

    suspend fun scheduleUpload(uploadId: String)

    suspend fun cancelUpload(uploadId: String)

    suspend fun publishVideo(uploadId: String): Result<Unit>
}
