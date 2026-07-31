package com.shortvideo.data.remote

import com.shortvideo.data.remote.dto.CancelUploadResponseDto
import com.shortvideo.data.remote.dto.CreateUploadRequestDto
import com.shortvideo.data.remote.dto.CreateUploadResponseDto
import com.shortvideo.data.remote.dto.DevUploadCompleteResponseDto
import com.shortvideo.data.remote.dto.ApiEnvelope
import com.shortvideo.data.remote.dto.PublishVideoRequestDto
import com.shortvideo.data.remote.dto.PublishVideoResponseDto
import com.shortvideo.data.remote.dto.UploadProgressRequestDto
import com.shortvideo.data.remote.dto.UploadProgressResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface UploadApi {
    @POST("v1/uploads")
    suspend fun createUpload(
        @Body body: CreateUploadRequestDto,
    ): ApiEnvelope<CreateUploadResponseDto>

    @PATCH("v1/uploads/{uploadId}/progress")
    suspend fun reportProgress(
        @Path("uploadId") uploadId: String,
        @Header("X-Upload-Token") uploadToken: String,
        @Body body: UploadProgressRequestDto,
    ): ApiEnvelope<UploadProgressResponseDto>

    @POST("v1/uploads/{uploadId}/dev-complete")
    suspend fun devComplete(
        @Path("uploadId") uploadId: String,
        @Header("X-Upload-Token") uploadToken: String,
    ): ApiEnvelope<DevUploadCompleteResponseDto>

    @POST("v1/videos/{videoId}/publish")
    suspend fun publishVideo(
        @Path("videoId") videoId: String,
        @Body body: PublishVideoRequestDto,
    ): ApiEnvelope<PublishVideoResponseDto>

    @DELETE("v1/uploads/{uploadId}")
    suspend fun cancelUpload(
        @Path("uploadId") uploadId: String,
    ): ApiEnvelope<CancelUploadResponseDto>
}
