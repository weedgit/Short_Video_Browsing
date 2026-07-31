package com.shortvideo.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FeedPageDto(
    @SerializedName("items") val items: List<FeedVideoDto>,
    @SerializedName("nextCursor") val nextCursor: String?,
    @SerializedName("hasMore") val hasMore: Boolean,
)

data class FeedVideoDto(
    @SerializedName("id") val id: String,
    @SerializedName("streamUrl") val streamUrl: String,
    @SerializedName("playbackFormat") val playbackFormat: String,
    @SerializedName("streamUrlExpiresAt") val streamUrlExpiresAt: String?,
    @SerializedName("authorName") val authorName: String,
    @SerializedName("description") val description: String,
    @SerializedName("hashtags") val hashtags: List<String>,
    @SerializedName("category") val category: String?,
    @SerializedName("uploadedAtLabel") val uploadedAtLabel: String,
    @SerializedName("durationMs") val durationMs: Long,
)

data class PlaybackEventRequestDto(
    @SerializedName("videoId") val videoId: String,
    @SerializedName("eventType") val eventType: String,
    @SerializedName("positionMs") val positionMs: Long,
    @SerializedName("occurredAt") val occurredAt: String? = null,
)

data class PlaybackBatchRequestDto(
    @SerializedName("events") val events: List<PlaybackEventRequestDto>,
)

data class PlaybackBatchResponseDto(
    @SerializedName("accepted") val accepted: Int,
)
