package com.example.pulsar.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val workId: String, // WorkManager UUID as String
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val filePath: String,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Int = 0, // 0 to 100
    val speed: String = "", // e.g., "1.2 MB/s"
    val eta: String = "", // e.g., "00:02:30"
    val isRecent: Boolean = true
)