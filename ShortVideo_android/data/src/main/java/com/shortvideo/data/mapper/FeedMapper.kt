package com.shortvideo.data.mapper

import com.shortvideo.data.remote.dto.FeedPageDto
import com.shortvideo.data.remote.dto.FeedVideoDto
import com.shortvideo.domain.model.FeedPage
import com.shortvideo.domain.model.FeedVideo

fun FeedPageDto.toDomain(): FeedPage =
    FeedPage(
        videos = items.map { it.toDomain() },
        nextCursor = nextCursor,
        hasMore = hasMore,
    )

fun FeedVideoDto.toDomain(): FeedVideo =
    FeedVideo(
        id = id,
        streamUrl = streamUrl,
        authorName = authorName,
        description = description,
        hashtags = hashtags,
        category = category,
        uploadedAtLabel = uploadedAtLabel,
        durationMs = durationMs,
        playbackFormat = playbackFormat,
        streamUrlExpiresAt = streamUrlExpiresAt,
    )

fun List<FeedVideo>.toMockPage(): FeedPage =
    FeedPage(
        videos = this,
        nextCursor = null,
        hasMore = false,
    )
