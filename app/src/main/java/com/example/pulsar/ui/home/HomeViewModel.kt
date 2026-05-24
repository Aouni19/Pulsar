package com.example.pulsar.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.pulsar.data.model.VideoInfo
import com.example.pulsar.domain.usecase.FetchVideoInfoUseCase
import com.example.pulsar.worker.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.pulsar.data.model.DownloadStatus
import androidx.work.WorkInfo

data class RecentDownloadItem(
    val id: String,
    val title: String,
    val quality: String,
    val status: DownloadStatus,
    val thumbnailUrl: String,
    val filePath: String? = null
)

sealed interface HomeUiState {
    object Idle : HomeUiState
    object Loading : HomeUiState
    data class Success(val videoInfo: VideoInfo) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

/**
 * ViewModel responsible for the main home screen logic, including fetching video details and starting downloads.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fetchVideoInfoUseCase: FetchVideoInfoUseCase,
    @ApplicationContext private val context: Context,
    private val settingsManager: com.example.pulsar.data.local.SettingsManager,
    private val downloadDao: com.example.pulsar.data.db.DownloadDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val videoQuality: StateFlow<String> = settingsManager.videoQualityFlow
    val audioQuality: StateFlow<String> = settingsManager.audioQualityFlow

    val recentDownloadsFlow = downloadDao.getAllDownloads()
        .map { records ->
            records.take(10).map { record ->
                RecentDownloadItem(
                    id = record.workId,
                    title = record.title,
                    quality = "Media",
                    status = record.status,
                    thumbnailUrl = record.thumbnailUrl,
                    filePath = record.filePath
                )
            }
        }

    fun fetchVideo(url: String) {
        if (url.isBlank()) {
            _uiState.value = HomeUiState.Error("Please enter a valid YouTube URL")
            return
        }

        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            val result = fetchVideoInfoUseCase(url)

            result.fold(
                onSuccess = { videoInfo ->
                    _uiState.value = HomeUiState.Success(videoInfo)
                },
                onFailure = { error ->
                    _uiState.value = HomeUiState.Error(error.message ?: "Failed to fetch video")
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = HomeUiState.Idle
    }

    fun startDownload(url: String, title: String, formatId: String, isAudio: Boolean, isVideoOnly: Boolean = false, quality: String, thumbnailUrl: String = "") {
        val workData = workDataOf(
            "URL" to url,
            "TITLE" to title,
            "FORMAT_ID" to formatId,
            "IS_AUDIO" to isAudio,
            "IS_VIDEO_ONLY" to isVideoOnly
        )

        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workData)
            .addTag("pulsar_download")
            .build()

        viewModelScope.launch {
            val record = com.example.pulsar.data.model.DownloadRecord(
                workId = downloadRequest.id.toString(),
                videoId = url,
                title = title,
                thumbnailUrl = thumbnailUrl,
                filePath = "",
                status = com.example.pulsar.data.model.DownloadStatus.QUEUED
            )
            downloadDao.insert(record)
            WorkManager.getInstance(context).enqueue(downloadRequest)
        }
        resetState()
    }
}