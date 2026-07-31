package com.shortvideo.data.repository

import com.shortvideo.data.local.dao.UploadSessionDao
import com.shortvideo.data.mapper.toDomain
import com.shortvideo.data.mapper.toEntity
import com.shortvideo.data.remote.ApiErrorParser
import com.shortvideo.data.remote.UploadApi
import com.shortvideo.data.remote.dto.CreateUploadRequestDto
import com.shortvideo.data.remote.dto.PublishVideoRequestDto
import com.shortvideo.data.upload.UploadWorkScheduler
import com.shortvideo.data.upload.VideoFileInspector
import com.shortvideo.domain.model.PublishVideoRequest
import com.shortvideo.domain.model.UploadSession
import com.shortvideo.domain.model.VideoFileInfo
import com.shortvideo.domain.repository.UploadRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class UploadRepositoryImpl @Inject constructor(
    private val uploadApi: UploadApi,
    private val uploadSessionDao: UploadSessionDao,
    private val videoFileInspector: VideoFileInspector,
    private val uploadWorkScheduler: UploadWorkScheduler,
) : UploadRepository {
    override suspend fun inspectVideo(uri: String): Result<VideoFileInfo> =
        runCatching { videoFileInspector.inspect(uri).getOrThrow() }
            .recoverCatching { error -> throw IllegalArgumentException(error.message ?: "Invalid video") }

    override suspend fun createUploadSession(
        video: VideoFileInfo,
        publish: PublishVideoRequest,
    ): Result<UploadSession> =
        runCatching {
            val response = uploadApi.createUpload(
                CreateUploadRequestDto(
                    mimeType = video.mimeType,
                    fileSizeBytes = video.fileSizeBytes,
                    durationMs = video.durationMs,
                ),
            ).data ?: error("Empty upload response")

            val entity = response.toEntity(video, publish)
            uploadSessionDao.upsert(entity)
            entity.toDomain()
        }.recoverCatching { error ->
            throw IllegalStateException(ApiErrorParser.messageFrom(error))
        }

    override fun observeSession(uploadId: String): Flow<UploadSession?> =
        uploadSessionDao.observeById(uploadId).map { entity -> entity?.toDomain() }

    override fun observeActiveSession(): Flow<UploadSession?> =
        uploadSessionDao.observeActive().map { entity -> entity?.toDomain() }

    override suspend fun getSession(uploadId: String): UploadSession? =
        uploadSessionDao.getById(uploadId)?.toDomain()

    override suspend fun scheduleUpload(uploadId: String) {
        uploadWorkScheduler.enqueue(uploadId)
    }

    override suspend fun cancelUpload(uploadId: String) {
        uploadWorkScheduler.cancel(uploadId)
        runCatching {
            uploadApi.cancelUpload(uploadId)
        }
        uploadSessionDao.deleteById(uploadId)
    }

    override suspend fun publishVideo(uploadId: String): Result<Unit> =
        runCatching {
            val session = uploadSessionDao.getById(uploadId)?.toDomain()
                ?: error("Upload session not found")
            uploadApi.publishVideo(
                videoId = session.videoId,
                body = PublishVideoRequestDto(
                    description = session.description,
                    hashtags = session.hashtags,
                    category = session.category,
                ),
            )
            Unit
        }.recoverCatching { error ->
            throw IllegalStateException(ApiErrorParser.messageFrom(error))
        }
}
