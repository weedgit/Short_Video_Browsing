package com.shortvideo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shortvideo.data.local.entity.UploadSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: UploadSessionEntity)

    @Update
    suspend fun update(session: UploadSessionEntity)

    @Query("SELECT * FROM upload_sessions WHERE uploadId = :uploadId LIMIT 1")
    fun observeById(uploadId: String): Flow<UploadSessionEntity?>

    @Query("SELECT * FROM upload_sessions WHERE uploadId = :uploadId LIMIT 1")
    suspend fun getById(uploadId: String): UploadSessionEntity?

    @Query(
        """
        SELECT * FROM upload_sessions
        WHERE status IN ('DRAFT', 'UPLOADING', 'UPLOADED', 'PROCESSING')
        ORDER BY createdAtMs DESC
        LIMIT 1
        """,
    )
    fun observeActive(): Flow<UploadSessionEntity?>

    @Query("UPDATE upload_sessions SET bytesUploaded = :bytesUploaded, status = :status WHERE uploadId = :uploadId")
    suspend fun updateProgress(uploadId: String, bytesUploaded: Long, status: String)

    @Query("UPDATE upload_sessions SET status = :status, errorMessage = :errorMessage WHERE uploadId = :uploadId")
    suspend fun updateStatus(uploadId: String, status: String, errorMessage: String?)

    @Query("DELETE FROM upload_sessions WHERE uploadId = :uploadId")
    suspend fun deleteById(uploadId: String)
}
