package com.shortvideo.data.upload

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.shortvideo.data.remote.UploadApi
import com.shortvideo.data.remote.dto.UploadProgressRequestDto
import com.shortvideo.domain.model.UploadSession
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

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
        if (isDevUploadUrl(session.uploadUrl) || session.provider == "dev") {
            uploadDevMode(session, onProgress)
            return
        }

        if (!session.uploadAuth.isNullOrBlank() && !session.uploadAddress.isNullOrBlank()) {
            uploadToAlibabaOss(session, onProgress)
            return
        }

        // Fallback: simple PUT (legacy)
        uploadSimplePut(session, onProgress)
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

    /**
     * Alibaba VOD client upload: PUT object to OSS using UploadAuth + UploadAddress.
     */
    private suspend fun uploadToAlibabaOss(
        session: UploadSession,
        onProgress: suspend (bytesUploaded: Long) -> Unit,
    ) {
        val addressJson = String(Base64.decode(session.uploadAddress, Base64.DEFAULT))
        val authJson = String(Base64.decode(session.uploadAuth, Base64.DEFAULT))
        val address = JSONObject(addressJson)
        val auth = JSONObject(authJson)

        val endpoint = address.optString("Endpoint").removePrefix("https://").removePrefix("http://")
        val bucket = address.optString("Bucket")
        val objectKey = address.optString("FileName")
        val accessKeyId = auth.optString("AccessKeyId")
        val accessKeySecret = auth.optString("AccessKeySecret")
        val securityToken = auth.optString("SecurityToken")

        if (endpoint.isBlank() || bucket.isBlank() || objectKey.isBlank() || accessKeyId.isBlank()) {
            throw IOException("Invalid Alibaba VOD upload credentials.")
        }

        val uploadUrl = "https://$bucket.$endpoint/$objectKey"
        val uri = Uri.parse(session.localUri)
        val mimeType = session.mimeType.ifBlank { "application/octet-stream" }
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Unable to open video file.")

        val date = gmtDate()
        val canonicalResource = "/$bucket/$objectKey"
        val headersToSign = "x-oss-security-token:$securityToken\n"
        val stringToSign = "PUT\n\n$mimeType\n$date\n$headersToSign$canonicalResource"
        val signature = signOss(accessKeySecret, stringToSign)

        val request = Request.Builder()
            .url(uploadUrl)
            .put(bytes.toRequestBody(mimeType.toMediaType()))
            .header("Date", date)
            .header("Content-Type", mimeType)
            .header("x-oss-security-token", securityToken)
            .header("Authorization", "OSS $accessKeyId:$signature")
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Alibaba OSS upload failed (${response.code}): ${response.body?.string()}")
        }

        uploadApi.reportProgress(
            uploadId = session.uploadId,
            uploadToken = session.uploadToken,
            body = UploadProgressRequestDto(bytesUploaded = bytes.size.toLong()),
        )
        onProgress(bytes.size.toLong())
    }

    private suspend fun uploadSimplePut(
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
                    .put(chunk.toRequestBody(mimeType))
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

    private fun gmtDate(): String {
        val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("GMT")
        return format.format(Date())
    }

    private fun signOss(accessKeySecret: String, stringToSign: String): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(accessKeySecret.toByteArray(Charsets.UTF_8), "HmacSHA1"))
        val raw = mac.doFinal(stringToSign.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(raw, Base64.NO_WRAP)
    }

    private fun isDevUploadUrl(uploadUrl: String): Boolean =
        uploadUrl.contains("/dev/") ||
            uploadUrl.contains("vod.aliyuncs.com/dev") ||
            uploadUrl.contains("cloudflarestream.com/dev")
}
