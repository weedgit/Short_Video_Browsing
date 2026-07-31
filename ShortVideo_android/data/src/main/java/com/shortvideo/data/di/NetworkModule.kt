package com.shortvideo.data.di

import com.shortvideo.data.BuildConfig
import com.shortvideo.data.network.AuthInterceptor
import com.shortvideo.data.network.DeviceHeadersInterceptor
import com.shortvideo.data.network.TokenAuthenticator
import com.shortvideo.data.remote.AuthApi
import com.shortvideo.data.remote.DiscoverApi
import com.shortvideo.data.remote.FeedApi
import com.shortvideo.data.remote.InboxApi
import com.shortvideo.data.remote.ProfileApi
import com.shortvideo.data.remote.SocialApi
import com.shortvideo.data.remote.UploadApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    @Provides
    @Singleton
    @Named("plainOkHttp")
    fun providePlainOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        deviceHeadersInterceptor: DeviceHeadersInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(deviceHeadersInterceptor)
            .build()

    @Provides
    @Singleton
    @Named("plainAuthApi")
    fun providePlainAuthApi(
        @Named("plainOkHttp") okHttpClient: OkHttpClient,
        @Named("apiBaseUrl") apiBaseUrl: String,
    ): AuthApi = createAuthApi(okHttpClient, apiBaseUrl)

    @Provides
    @Singleton
    fun provideAuthenticatedOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        deviceHeadersInterceptor: DeviceHeadersInterceptor,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(deviceHeadersInterceptor)
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(
        okHttpClient: OkHttpClient,
        @Named("apiBaseUrl") apiBaseUrl: String,
    ): AuthApi = createRetrofitApi(okHttpClient, apiBaseUrl, AuthApi::class.java)

    @Provides
    @Singleton
    fun provideFeedApi(
        okHttpClient: OkHttpClient,
        @Named("apiBaseUrl") apiBaseUrl: String,
    ): FeedApi = createRetrofitApi(okHttpClient, apiBaseUrl, FeedApi::class.java)

    @Provides
    @Singleton
    fun provideUploadApi(
        okHttpClient: OkHttpClient,
        @Named("apiBaseUrl") apiBaseUrl: String,
    ): UploadApi = createRetrofitApi(okHttpClient, apiBaseUrl, UploadApi::class.java)

    @Provides
    @Singleton
    fun provideSocialApi(
        okHttpClient: OkHttpClient,
        @Named("apiBaseUrl") apiBaseUrl: String,
    ): SocialApi = createRetrofitApi(okHttpClient, apiBaseUrl, SocialApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApi(
        okHttpClient: OkHttpClient,
        @Named("apiBaseUrl") apiBaseUrl: String,
    ): ProfileApi = createRetrofitApi(okHttpClient, apiBaseUrl, ProfileApi::class.java)

    @Provides
    @Singleton
    fun provideDiscoverApi(
        okHttpClient: OkHttpClient,
        @Named("apiBaseUrl") apiBaseUrl: String,
    ): DiscoverApi = createRetrofitApi(okHttpClient, apiBaseUrl, DiscoverApi::class.java)

    @Provides
    @Singleton
    fun provideInboxApi(
        okHttpClient: OkHttpClient,
        @Named("apiBaseUrl") apiBaseUrl: String,
    ): InboxApi = createRetrofitApi(okHttpClient, apiBaseUrl, InboxApi::class.java)

    private fun <T> createRetrofitApi(
        okHttpClient: OkHttpClient,
        apiBaseUrl: String,
        apiClass: Class<T>,
    ): T =
        Retrofit.Builder()
            .baseUrl(apiBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(apiClass)

    private fun createAuthApi(okHttpClient: OkHttpClient, apiBaseUrl: String): AuthApi =
        createRetrofitApi(okHttpClient, apiBaseUrl, AuthApi::class.java)
}
