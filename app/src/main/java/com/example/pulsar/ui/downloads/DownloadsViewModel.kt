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

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import com.example.pulsar.data.local.SettingsManager

data class DownloadUiItem(
    val id: String,
    val title: String,
    val quality: String,
    val progress: Float,
    val progressText: String,
    val statusText: String,
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
    private val settingsManager: SettingsManager,
    private val downloadDao: com.example.pulsar.data.db.DownloadDao
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)
    private val deletedIds = MutableStateFlow(settingsManager.getDeletedDownloadIds())

    val downloadsFlow: Flow<List<DownloadUiItem>> = downloadDao.getAllDownloads()
        .map { records ->
            records.filter { it.workId !in deletedIds.value }.map { record ->
                DownloadUiItem(
                    id = record.workId,
                    title = record.title,
                    quality = "Media",
                    progress = record.progress / 100f,
                    progressText = "${record.progress}%",
                    statusText = when (record.status) {
                        com.example.pulsar.data.model.DownloadStatus.COMPLETED -> "Completed"
                        com.example.pulsar.data.model.DownloadStatus.FAILED -> "Failed"
                        com.example.pulsar.data.model.DownloadStatus.CANCELLED -> "Cancelled"
                        com.example.pulsar.data.model.DownloadStatus.QUEUED -> "Queued"
                        com.example.pulsar.data.model.DownloadStatus.DOWNLOADING -> {
                            if (record.speed.isNotEmpty() && record.eta.isNotEmpty()) "${record.speed} • ${record.eta}"
                            else if (record.speed.isNotEmpty()) record.speed
                            else "Starting..."
                        }
                    },
                    isPlaying = record.status == com.example.pulsar.data.model.DownloadStatus.DOWNLOADING,
                    isQueued = record.status == com.example.pulsar.data.model.DownloadStatus.QUEUED,
                    thumbnailUrl = record.thumbnailUrl,
                    filePath = record.filePath
                )
            }
        }

    fun cancelDownload(id: String) {
        // 1. Tell WorkManager to politely stop tracking
        workManager.cancelWorkById(UUID.fromString(id))
        
        // 2. Immediate UI Update: Mark as cancelled in DB
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Kill the native process instantly
                YoutubeDL.getInstance().destroyProcessById("Task_$id")
                
                // Update DB status so UI reacts immediately
                downloadDao.getDownloadByWorkId(id)?.let { record ->
                    downloadDao.update(record.copy(
                        status = com.example.pulsar.data.model.DownloadStatus.CANCELLED,
                        speed = "",
                        eta = ""
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteDownload(id: String, filePath: String?) {
        // 1. Cancel the work if it's running
        cancelDownload(id)

        // Hide it from the UI
        settingsManager.addDeletedDownloadId(id)
        deletedIds.value = settingsManager.getDeletedDownloadIds()

        // Delete the actual file from storage
        viewModelScope.launch(Dispatchers.IO) {
            try {
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