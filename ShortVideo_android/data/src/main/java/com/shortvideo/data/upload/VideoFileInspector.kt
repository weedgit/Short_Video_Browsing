package com.shortvideo.data.upload

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.shortvideo.domain.model.VideoFileInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Accepts Alibaba ApsaraVideo VOD supported video containers only.
 * @see https://www.alibabacloud.com/help/en/vod/product-overview/audio-and-video-upload
 */
private const val MAX_FILE_SIZE_BYTES = 1_073_741_824L
private const val MAX_DURATION_MS = 600_000L
private const val STREAM_BUFFER_SIZE = 8192

private val ALIBABA_VOD_VIDEO_EXTENSIONS = setOf(
    "3gp", "asf", "avi", "dat", "dv", "f4v", "flv",
    "m2t", "m4v", "mj2", "mjpeg", "mkv", "mov", "mp4",
    "mpe", "mpeg", "mpg", "mts", "ogg", "ogv", "qt",
    "rm", "rmvb", "ts", "vob", "webm", "wmv",
)

private val ALIBABA_VOD_VIDEO_MIME_TYPES = setOf(
    "video/mp4",
    "video/quicktime",
    "video/webm",
    "video/x-msvideo",
    "video/avi",
    "video/msvideo",
    "video/x-matroska",
    "video/mpeg",
    "video/mpg",
    "video/3gpp",
    "video/3gpp2",
    "video/x-flv",
    "video/flv",
    "video/x-f4v",
    "video/x-ms-wmv",
    "video/x-ms-asf",
    "video/mp2t",
    "video/ogg",
    "video/dvd",
    "video/x-ms-vob",
    "video/x-dv",
    "video/dv",
    "video/x-pn-realvideo",
    "application/vnd.rn-realmedia",
    "application/vnd.rn-realmedia-vbr",
)

private const val FORMAT_HINT = "MP4, MOV, AVI, MKV, WEBM, FLV, 3GP, MPEG, TS, VOB, WMV"

@Singleton
class VideoFileInspector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun inspect(uriString: String): Result<VideoFileInfo> {
        val uri = Uri.parse(uriString)
        val resolver = context.contentResolver
        val displayName = queryDisplayName(resolver, uri)
        val mimeType = resolveMimeType(resolver, uri, displayName)
            ?: return Result.failure(
                IllegalArgumentException(
                    "Unsupported file type. Use an Alibaba VOD format ($FORMAT_HINT).",
                ),
            )

        if (!isAlibabaVodVideoMimeType(mimeType)) {
            return Result.failure(
                IllegalArgumentException(
                    "Only Alibaba VOD video formats are supported ($FORMAT_HINT).",
                ),
            )
        }

        val fileSizeBytes = resolveFileSizeBytes(resolver, uri)
        if (fileSizeBytes <= 0) {
            return Result.failure(IllegalArgumentException("Unable to read file size."))
        }
        if (fileSizeBytes > MAX_FILE_SIZE_BYTES) {
            return Result.failure(IllegalArgumentException("File exceeds the 1GB upload limit."))
        }

        val durationMs = readDurationMs(uri)
        if (durationMs > MAX_DURATION_MS) {
            return Result.failure(IllegalArgumentException("Video exceeds the 10 minute limit."))
        }

        // Duration may be 0 for some containers (e.g. uncommon codecs); API treats it as optional.
        return Result.success(
            VideoFileInfo(
                uri = uriString,
                mimeType = mimeType,
                fileSizeBytes = fileSizeBytes,
                durationMs = durationMs.coerceAtLeast(0L),
            ),
        )
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    return cursor.getString(index)
                }
            }
        }
        return uri.lastPathSegment
    }

    private fun resolveMimeType(
        resolver: ContentResolver,
        uri: Uri,
        displayName: String?,
    ): String? {
        val resolverType = resolver.getType(uri)?.takeUnless { isGenericBinaryType(it) }
        if (resolverType != null) {
            return normalizeMimeType(resolverType, displayName)
        }

        val extension = displayName
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: MimeTypeMap.getFileExtensionFromUrl(uri.toString())?.lowercase()

        if (!extension.isNullOrBlank()) {
            return normalizeMimeType(
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension),
                displayName,
            )
        }

        return null
    }

    private fun normalizeMimeType(mimeType: String?, displayName: String?): String? {
        if (mimeType != null && isAlibabaVodVideoMimeType(mimeType)) {
            return mimeType.lowercase()
        }

        val extension = displayName
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }

        return mimeFromExtension(extension)
            ?: mimeType?.takeIf { isAlibabaVodVideoMimeType(it) }?.lowercase()
    }

    private fun mimeFromExtension(extension: String?): String? {
        if (extension.isNullOrBlank() || extension !in ALIBABA_VOD_VIDEO_EXTENSIONS) {
            return null
        }
        return when (extension) {
            "mp4", "m4v" -> "video/mp4"
            "mov", "qt" -> "video/quicktime"
            "webm" -> "video/webm"
            "avi" -> "video/x-msvideo"
            "mkv" -> "video/x-matroska"
            "mpeg", "mpg", "mpe", "dat", "mjpeg", "mj2" -> "video/mpeg"
            "vob" -> "video/dvd"
            "3gp" -> "video/3gpp"
            "flv", "f4v" -> "video/x-flv"
            "wmv" -> "video/x-ms-wmv"
            "asf" -> "video/x-ms-asf"
            "ts", "m2t", "mts" -> "video/mp2t"
            "ogg", "ogv" -> "video/ogg"
            "rm", "rmvb" -> "application/vnd.rn-realmedia"
            "dv" -> "video/x-dv"
            else -> null
        }
    }

    private fun isAlibabaVodVideoMimeType(mimeType: String): Boolean =
        mimeType.lowercase() in ALIBABA_VOD_VIDEO_MIME_TYPES

    private fun isGenericBinaryType(mimeType: String): Boolean =
        mimeType == "application/octet-stream" || mimeType == "binary/octet-stream"

    private fun resolveFileSizeBytes(resolver: ContentResolver, uri: Uri): Long {
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0) {
                    val size = cursor.getLong(index)
                    if (size > 0) return size
                }
            }
        }

        resolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            if (descriptor.length > 0) return descriptor.length
        }

        return countStreamSize(resolver, uri)
    }

    private fun countStreamSize(resolver: ContentResolver, uri: Uri): Long {
        var total = 0L
        resolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(STREAM_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                total += read
                if (total > MAX_FILE_SIZE_BYTES) break
            }
        }
        return total
    }

    private fun readDurationMs(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }
}
