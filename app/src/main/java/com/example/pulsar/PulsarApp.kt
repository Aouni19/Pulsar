package com.example.pulsar

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class for Pulsar. Initializes background libraries and services.
 */
@HiltAndroidApp
class PulsarApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Initialize libraries safely
                try {
                    YoutubeDL.getInstance().init(this@PulsarApp)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    FFmpeg.getInstance().init(this@PulsarApp)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    Aria2c.getInstance().init(this@PulsarApp)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Update yt-dlp to the latest stable version (non-blocking, safe to fail)
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(this@PulsarApp, YoutubeDL.UpdateChannel.STABLE)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}