package com.shortvideo.data.remote

import com.shortvideo.data.remote.dto.ApiEnvelope
import com.shortvideo.data.remote.dto.FeedPageDto
import com.shortvideo.data.remote.dto.PlaybackBatchRequestDto
import com.shortvideo.data.remote.dto.PlaybackBatchResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface FeedApi {
    @GET("v1/feed")
    suspend fun getFeed(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 10,
    ): ApiEnvelope<FeedPageDto>

    @POST("v1/playback/events/batch")
    suspend fun postPlaybackEventsBatch(
        @Body body: PlaybackBatchRequestDto,
    ): ApiEnvelope<PlaybackBatchResponseDto>
}
