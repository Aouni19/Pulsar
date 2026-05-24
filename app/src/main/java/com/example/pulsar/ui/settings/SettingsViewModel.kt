package com.example.pulsar.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.example.pulsar.data.local.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel managing the application settings, providing observable states for UI configuration.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _maxConcurrent = MutableStateFlow(settingsManager.getMaxConcurrent())
    val maxConcurrent: StateFlow<Int> = _maxConcurrent.asStateFlow()

    private val _useAria2c = MutableStateFlow(settingsManager.getUseAria2c())
    val useAria2c: StateFlow<Boolean> = _useAria2c.asStateFlow()

    private val _customFlags = MutableStateFlow(settingsManager.getCustomFlags())
    val customFlags: StateFlow<String> = _customFlags.asStateFlow()

    fun updateMaxConcurrent(value: Int) {
        settingsManager.setMaxConcurrent(value)
        _maxConcurrent.value = value
    }

    fun updateUseAria2c(value: Boolean) {
        settingsManager.setUseAria2c(value)
        _useAria2c.value = value
    }

    fun updateCustomFlags(flags: String) {
        settingsManager.setCustomFlags(flags)
        _customFlags.value = flags
    }

    fun clearCache() {
        val bytesDeleted = settingsManager.clearCache(context)
        val mbDeleted = bytesDeleted / (1024 * 1024)
        Toast.makeText(context, "Cleared $mbDeleted MB of temporary data", Toast.LENGTH_SHORT).show()
    }

    private val _videoQuality = MutableStateFlow(settingsManager.getVideoQuality())
    val videoQuality: StateFlow<String> = _videoQuality.asStateFlow()

    private val _audioQuality = MutableStateFlow(settingsManager.getAudioQuality())
    val audioQuality: StateFlow<String> = _audioQuality.asStateFlow()

    fun updateVideoQuality(quality: String) {
        settingsManager.setVideoQuality(quality)
        _videoQuality.value = quality
    }

    fun updateAudioQuality(quality: String) {
        settingsManager.setAudioQuality(quality)
        _audioQuality.value = quality
    }

    private val _theme = MutableStateFlow(settingsManager.getTheme())
    val theme: StateFlow<String> = _theme.asStateFlow()

    fun updateTheme(theme: String) {
        settingsManager.setTheme(theme)
        _theme.value = theme
    }
}