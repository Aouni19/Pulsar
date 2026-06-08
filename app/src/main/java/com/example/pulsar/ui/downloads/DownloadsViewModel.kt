package com.example.pulsar.ui.downloads

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.viewModelScope
import java.util.UUID
import javax.inject.Inject

import java.io.File
import com.example.pulsar.data.model.DownloadStatus

data class DownloadUiItem(
    val id: String,
    val title: String,
    val quality: String,
    val progress: Float,
    val progressText: String,
    val statusText: String,
    val status: DownloadStatus,
    val isPlaying: Boolean,
    val isQueued: Boolean,
    val thumbnailUrl: String,
    val filePath: String? = null
)

/**
 * ViewModel managing the active and queued downloads list, allowing cancellation and deletion.
 */
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: com.example.pulsar.data.db.DownloadDao
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    val downloadsFlow: Flow<List<DownloadUiItem>> = downloadDao.getAllDownloads()
        .map { records ->
            records.map { record ->
                DownloadUiItem(
                    id = record.workId,
                    title = record.title,
                    quality = "Media",
                    progress = record.progress / 100f,
                    progressText = "${record.progress}%",
                    statusText = when (record.status) {
                        DownloadStatus.COMPLETED -> "Completed"
                        DownloadStatus.FAILED -> "Failed"
                        DownloadStatus.CANCELLED -> "Cancelled"
                        DownloadStatus.QUEUED -> "Queued"
                        DownloadStatus.DOWNLOADING -> {
                            if (record.speed.isNotEmpty() && record.eta.isNotEmpty()) "${record.speed} • ${record.eta}"
                            else if (record.speed.isNotEmpty()) record.speed
                            else "Starting..."
                        }
                    },
                    status = record.status,
                    isPlaying = record.status == DownloadStatus.DOWNLOADING,
                    isQueued = record.status == DownloadStatus.QUEUED,
                    thumbnailUrl = record.thumbnailUrl,
                    filePath = record.filePath
                )
            }
        }

    fun cancelDownload(id: String) {
        // 1. Tell WorkManager to politely stop tracking
        try {
            workManager.cancelWorkById(UUID.fromString(id))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 2. Immediate UI Update: Mark as cancelled in DB
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Kill the native process instantly
                try {
                    YoutubeDL.getInstance().destroyProcessById("Task_$id")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // Update DB status so UI reacts immediately
                downloadDao.getDownloadByWorkId(id)?.let { record ->
                    if (record.status == DownloadStatus.DOWNLOADING || 
                        record.status == DownloadStatus.QUEUED) {
                        downloadDao.update(record.copy(
                            status = DownloadStatus.CANCELLED,
                            speed = "",
                            eta = ""
                        ))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteDownload(id: String, filePath: String?) {
        // 1. Cancel the work if it's running
        try {
            workManager.cancelWorkById(UUID.fromString(id))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Delete from the database
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Kill the native process instantly if it's downloading
                try {
                    YoutubeDL.getInstance().destroyProcessById("Task_$id")
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                downloadDao.deleteByWorkId(id)
                
                // Delete the actual file from storage
                filePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}