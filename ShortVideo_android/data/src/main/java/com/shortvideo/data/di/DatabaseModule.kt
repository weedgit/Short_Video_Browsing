package com.shortvideo.data.di

import android.content.Context
import androidx.room.Room
import com.shortvideo.data.local.AuthDatabase
import com.shortvideo.data.local.dao.AuthTokenDao
import com.shortvideo.data.local.dao.UploadSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAuthDatabase(
        @ApplicationContext context: Context,
    ): AuthDatabase =
        Room.databaseBuilder(
            context,
            AuthDatabase::class.java,
            "shortvideo_auth.db",
        ).fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideAuthTokenDao(database: AuthDatabase): AuthTokenDao = database.authTokenDao()

    @Provides
    @Singleton
    fun provideUploadSessionDao(database: AuthDatabase): UploadSessionDao = database.uploadSessionDao()
}
