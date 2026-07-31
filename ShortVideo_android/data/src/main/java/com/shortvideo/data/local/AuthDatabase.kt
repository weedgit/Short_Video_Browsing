package com.shortvideo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shortvideo.data.local.dao.AuthTokenDao
import com.shortvideo.data.local.dao.UploadSessionDao
import com.shortvideo.data.local.entity.AuthTokenEntity
import com.shortvideo.data.local.entity.UploadSessionEntity

@Database(
    entities = [AuthTokenEntity::class, UploadSessionEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AuthDatabase : RoomDatabase() {
    abstract fun authTokenDao(): AuthTokenDao

    abstract fun uploadSessionDao(): UploadSessionDao
}
