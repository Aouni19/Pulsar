package com.example.pulsar.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pulsar.data.model.DownloadRecord
import com.example.pulsar.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for download records.
 */
@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DownloadRecord): Long

    // Fix: Explicitly return Int (number of rows updated)
    @Update
    suspend fun update(record: DownloadRecord): Int

    // Fix: Explicitly return Int to prevent KSP "V" signature crash
    @Query("UPDATE downloads SET progress = :progress, speed = :speed, eta = :eta, status = :status WHERE id = :id")
    suspend fun updateProgress(id: Int, progress: Int, speed: String, eta: String, status: DownloadStatus): Int

    @Query("SELECT * FROM downloads ORDER BY id DESC")
    fun getAllDownloads(): Flow<List<DownloadRecord>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Int): DownloadRecord?

    @Query("SELECT * FROM downloads WHERE workId = :workId")
    suspend fun getDownloadByWorkId(workId: String): DownloadRecord?

    @Query("DELETE FROM downloads WHERE workId = :workId")
    suspend fun deleteByWorkId(workId: String): Int

    @Query("UPDATE downloads SET isRecent = 0")
    suspend fun clearRecentHistory(): Int

    @Query("DELETE FROM downloads")
    suspend fun deleteAll(): Int
}