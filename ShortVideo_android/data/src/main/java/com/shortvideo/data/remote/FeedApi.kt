package com.shortvideo.data.remote

import com.shortvideo.data.remote.dto.ApiEnvelope
import com.shortvideo.data.remote.dto.CommentDto
import com.shortvideo.data.remote.dto.CommentListDto
import com.shortvideo.data.remote.dto.CreateCommentRequestDto
import com.shortvideo.data.remote.dto.DiscoverResponseDto
import com.shortvideo.data.remote.dto.FeedPageDto
import com.shortvideo.data.remote.dto.FollowResponseDto
import com.shortvideo.data.remote.dto.InboxListDto
import com.shortvideo.data.remote.dto.LikeResponseDto
import com.shortvideo.data.remote.dto.PlaybackBatchRequestDto
import com.shortvideo.data.remote.dto.PlaybackBatchResponseDto
import com.shortvideo.data.remote.dto.ProfileVideosDto
import com.shortvideo.data.remote.dto.RegisterDeviceRequestDto
import com.shortvideo.data.remote.dto.UpdateProfileRequestDto
import com.shortvideo.data.remote.dto.UserProfileDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface FeedApi {
    @GET("v1/feed")
    suspend fun getFeed(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 10,
        @Query("tab") tab: String = "foryou",
    ): ApiEnvelope<FeedPageDto>

    @POST("v1/playback/events/batch")
    suspend fun postPlaybackEventsBatch(
        @Body body: PlaybackBatchRequestDto,
    ): ApiEnvelope<PlaybackBatchResponseDto>
}

interface SocialApi {
    @POST("v1/videos/{videoId}/like")
    suspend fun likeVideo(@Path("videoId") videoId: String): ApiEnvelope<LikeResponseDto>

    @DELETE("v1/videos/{videoId}/like")
    suspend fun unlikeVideo(@Path("videoId") videoId: String): ApiEnvelope<LikeResponseDto>

    @GET("v1/videos/{videoId}/comments")
    suspend fun getComments(@Path("videoId") videoId: String): ApiEnvelope<CommentListDto>

    @POST("v1/videos/{videoId}/comments")
    suspend fun postComment(
        @Path("videoId") videoId: String,
        @Body body: CreateCommentRequestDto,
    ): ApiEnvelope<CommentDto>

    @POST("v1/users/{userId}/follow")
    suspend fun followUser(@Path("userId") userId: String): ApiEnvelope<FollowResponseDto>

    @DELETE("v1/users/{userId}/follow")
    suspend fun unfollowUser(@Path("userId") userId: String): ApiEnvelope<FollowResponseDto>

    @POST("v1/videos/{videoId}/save")
    suspend fun saveVideo(@Path("videoId") videoId: String): ApiEnvelope<Map<String, Boolean>>

    @DELETE("v1/videos/{videoId}/save")
    suspend fun unsaveVideo(@Path("videoId") videoId: String): ApiEnvelope<Map<String, Boolean>>
}

interface ProfileApi {
    @GET("v1/users/me/profile")
    suspend fun getMyProfile(): ApiEnvelope<UserProfileDto>

    @PATCH("v1/users/me/profile")
    suspend fun updateMyProfile(
        @Body body: UpdateProfileRequestDto,
    ): ApiEnvelope<UserProfileDto>

    @Multipart
    @POST("v1/users/me/avatar")
    suspend fun uploadAvatar(
        @Part avatar: MultipartBody.Part,
    ): ApiEnvelope<UserProfileDto>

    @GET("v1/users/{userId}/profile")
    suspend fun getProfile(@Path("userId") userId: String): ApiEnvelope<UserProfileDto>

    @GET("v1/users/{userId}/videos")
    suspend fun getProfileVideos(@Path("userId") userId: String): ApiEnvelope<ProfileVideosDto>
}

interface DiscoverApi {
    @GET("v1/discover")
    suspend fun discover(@Query("q") query: String? = null): ApiEnvelope<DiscoverResponseDto>
}

interface InboxApi {
    @GET("v1/inbox")
    suspend fun getInbox(): ApiEnvelope<InboxListDto>

    @POST("v1/inbox/{id}/read")
    suspend fun markRead(@Path("id") id: String): ApiEnvelope<Map<String, Boolean>>

    @POST("v1/inbox/read-all")
    suspend fun markAllRead(): ApiEnvelope<Map<String, Boolean>>

    @POST("v1/devices/fcm")
    suspend fun registerFcm(@Body body: RegisterDeviceRequestDto): ApiEnvelope<Map<String, Boolean>>
}
