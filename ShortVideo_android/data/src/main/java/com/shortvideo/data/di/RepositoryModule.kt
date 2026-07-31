package com.shortvideo.data.di

import com.shortvideo.data.repository.AuthRepositoryImpl
import com.shortvideo.data.repository.DiscoverRepositoryImpl
import com.shortvideo.data.repository.FeedPlaybackRepositoryImpl
import com.shortvideo.data.repository.FeedRepositoryImpl
import com.shortvideo.data.repository.InboxRepositoryImpl
import com.shortvideo.data.repository.PlaybackEventRepositoryImpl
import com.shortvideo.data.repository.ProfileRepositoryImpl
import com.shortvideo.data.repository.SocialRepositoryImpl
import com.shortvideo.data.repository.UploadRepositoryImpl
import com.shortvideo.domain.repository.AuthRepository
import com.shortvideo.domain.repository.DiscoverRepository
import com.shortvideo.domain.repository.FeedPlaybackRepository
import com.shortvideo.domain.repository.FeedRepository
import com.shortvideo.domain.repository.InboxRepository
import com.shortvideo.domain.repository.PlaybackEventRepository
import com.shortvideo.domain.repository.ProfileRepository
import com.shortvideo.domain.repository.SocialRepository
import com.shortvideo.domain.repository.UploadRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindFeedRepository(impl: FeedRepositoryImpl): FeedRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPlaybackEventRepository(
        impl: PlaybackEventRepositoryImpl,
    ): PlaybackEventRepository

    @Binds
    @Singleton
    abstract fun bindFeedPlaybackRepository(
        impl: FeedPlaybackRepositoryImpl,
    ): FeedPlaybackRepository

    @Binds
    @Singleton
    abstract fun bindUploadRepository(impl: UploadRepositoryImpl): UploadRepository

    @Binds
    @Singleton
    abstract fun bindSocialRepository(impl: SocialRepositoryImpl): SocialRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindDiscoverRepository(impl: DiscoverRepositoryImpl): DiscoverRepository

    @Binds
    @Singleton
    abstract fun bindInboxRepository(impl: InboxRepositoryImpl): InboxRepository
}
