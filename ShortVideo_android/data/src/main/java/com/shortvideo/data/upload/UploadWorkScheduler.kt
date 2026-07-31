package com.shortvideo.data.upload

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadWorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun enqueue(uploadId: String) {
        val request = OneTimeWorkRequestBuilder<VideoUploadWorker>()
            .setInputData(workDataOf(KEY_UPLOAD_ID to uploadId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(WORK_TAG)
            .addTag(uploadWorkTag(uploadId))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(uploadId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(uploadId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(uploadId))
    }

    companion object {
        const val KEY_UPLOAD_ID = "upload_id"
        const val WORK_TAG = "video_upload"

        fun uniqueWorkName(uploadId: String): String = "upload_$uploadId"

        fun uploadWorkTag(uploadId: String): String = "upload_id_$uploadId"
    }
}
