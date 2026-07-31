package com.shortvideo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shortvideo.data.local.entity.AuthTokenEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthTokenDao {
    @Query("SELECT * FROM auth_tokens WHERE id = :id LIMIT 1")
    fun observeToken(id: Int = AuthTokenEntity.SINGLETON_ID): Flow<AuthTokenEntity?>

    @Query("SELECT * FROM auth_tokens WHERE id = :id LIMIT 1")
    suspend fun getToken(id: Int = AuthTokenEntity.SINGLETON_ID): AuthTokenEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertToken(entity: AuthTokenEntity)

    @Query("DELETE FROM auth_tokens")
    suspend fun clearAll()
}
