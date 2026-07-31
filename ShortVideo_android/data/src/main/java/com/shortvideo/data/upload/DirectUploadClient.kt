package com.shortvideo.data.upload

import android.content.Context
import android.net.Uri
import com.shortvideo.data.remote.UploadApi
import com.shortvideo.data.remote.dto.UploadProgressRequestDto
import com.shortvideo.domain.model.UploadSession
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private const val CHUNK_SIZE_BYTES = 512 * 1024

@Singleton
class DirectUploadClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val uploadApi: UploadApi,
    @javax.inject.Named("plainOkHttp") private val okHttpClient: OkHttpClient,
) {
    suspend fun upload(
        session: UploadSession,
        onProgress: suspend (bytesUploaded: Long) -> Unit,
    ) {
        if (isDevUploadUrl(session.uploadUrl)) {
            uploadDevMode(session, onProgress)
            return
        }

        uploadToCloudflare(session, onProgress)
    }

    private suspend fun uploadDevMode(
        session: UploadSession,
        onProgress: suspend (bytesUploaded: Long) -> Unit,
    ) {
        var bytesUploaded = session.bytesUploaded
        while (bytesUploaded < session.fileSizeBytes) {
            bytesUploaded = minOf(bytesUploaded + CHUNK_SIZE_BYTES, session.fileSizeBytes)
            uploadApi.reportProgress(
                uploadId = session.uploadId,
                uploadToken = session.uploadToken,
                body = UploadProgressRequestDto(bytesUploaded = bytesUploaded),
            )
            onProgress(bytesUploaded)
            delay(50)
        }

        uploadApi.devComplete(
            uploadId = session.uploadId,
            uploadToken = session.uploadToken,
        )
    }

    private suspend fun uploadToCloudflare(
        session: UploadSession,
        onProgress: suspend (bytesUploaded: Long) -> Unit,
    ) {
        val uri = Uri.parse(session.localUri)
        val mimeType = session.mimeType.toMediaType()
        var bytesUploaded = session.bytesUploaded

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.skip(bytesUploaded)
            val buffer = ByteArray(CHUNK_SIZE_BYTES)

            while (bytesUploaded < session.fileSizeBytes) {
                val read = inputStream.read(buffer)
                if (read <= 0) break

                val chunk = buffer.copyOf(read)
                val request = Request.Builder()
                    .url(session.uploadUrl)
                    .post(chunk.toRequestBody(mimeType))
                    .header("Content-Range", "bytes $bytesUploaded-${bytesUploaded + read - 1}/${session.fileSizeBytes}")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Cloud upload failed (${response.code})")
                }

                bytesUploaded += read
                uploadApi.reportProgress(
                    uploadId = session.uploadId,
                    uploadToken = session.uploadToken,
                    body = UploadProgressRequestDto(bytesUploaded = bytesUploaded),
                )
                onProgress(bytesUploaded)
            }
        } ?: throw IOException("Unable to open video file.")

        if (bytesUploaded < session.fileSizeBytes) {
            throw IOException("Upload ended before the full file was sent.")
        }
    }

    private fun isDevUploadUrl(uploadUrl: String): Boolean =
        uploadUrl.contains("/dev/") || uploadUrl.contains("cloudflarestream.com/dev")
}
