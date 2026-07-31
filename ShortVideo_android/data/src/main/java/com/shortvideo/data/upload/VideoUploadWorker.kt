package com.shortvideo.data.upload

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.shortvideo.data.local.dao.UploadSessionDao
import com.shortvideo.data.mapper.toDomain
import com.shortvideo.data.remote.UploadApi
import com.shortvideo.data.remote.dto.PublishVideoRequestDto
import com.shortvideo.domain.model.UploadStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class VideoUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val uploadSessionDao: UploadSessionDao,
    private val directUploadClient: DirectUploadClient,
    private val uploadApi: UploadApi,
    private val uploadNotificationHelper: UploadNotificationHelper,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uploadId = inputData.getString(UploadWorkScheduler.KEY_UPLOAD_ID)
            ?: return@withContext Result.failure()

        val entity = uploadSessionDao.getById(uploadId)
            ?: return@withContext Result.failure()

        try {
            uploadSessionDao.updateStatus(uploadId, UploadStatus.UPLOADING.name, null)

            runCatching {
                setForeground(createForegroundInfo(entity.description.ifBlank { "Uploading video" }))
            }

            val session = entity.toDomain()

            directUploadClient.upload(session) { bytesUploaded ->
                uploadSessionDao.updateProgress(
                    uploadId = uploadId,
                    bytesUploaded = bytesUploaded,
                    status = if (bytesUploaded >= session.fileSizeBytes) {
                        UploadStatus.UPLOADED.name
                    } else {
                        UploadStatus.UPLOADING.name
                    },
                )
            }

            uploadSessionDao.updateStatus(uploadId, UploadStatus.PROCESSING.name, null)

            uploadApi.publishVideo(
                videoId = session.videoId,
                body = PublishVideoRequestDto(
                    description = session.description,
                    hashtags = session.hashtags,
                    category = session.category,
                ),
            )

            uploadSessionDao.updateStatus(uploadId, UploadStatus.PUBLISHED.name, null)
            Result.success()
        } catch (error: Exception) {
            uploadSessionDao.updateStatus(
                uploadId = uploadId,
                status = UploadStatus.FAILED.name,
                errorMessage = error.message ?: "Upload failed",
            )
            if (runAttemptCount < MAX_RETRY_COUNT) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun createForegroundInfo(title: String): ForegroundInfo =
        uploadNotificationHelper.buildForegroundInfo(
            notificationId = NOTIFICATION_ID,
            title = title,
        )

    private companion object {
        const val NOTIFICATION_ID = 1001
        const val MAX_RETRY_COUNT = 5
    }
}
