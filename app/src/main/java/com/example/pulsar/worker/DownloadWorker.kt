package com.example.pulsar.worker

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.pulsar.data.local.SettingsManager
import com.yausername.ffmpeg.FFmpeg
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

/**
 * Worker class responsible for executing background downloads using YoutubeDL and FFmpeg.
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val settings: SettingsManager,
    private val downloadDao: com.example.pulsar.data.db.DownloadDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString("URL") ?: return@withContext Result.failure()
        val formatId = inputData.getString("FORMAT_ID") ?: "best"
        val isAudio = inputData.getBoolean("IS_AUDIO", false)
        val title = inputData.getString("TITLE") ?: "Unknown Title"

        val processId = "Task_$id"

        try {
            YoutubeDL.getInstance().init(applicationContext)
            FFmpeg.getInstance().init(applicationContext)

            // 1. Grab user preferences from injected settings
            val maxConcurrent = settings.getMaxConcurrent()
            val useAria = settings.getUseAria2c()
            val customFlags = settings.getCustomFlags()

            // Update status to DOWNLOADING in DB
            downloadDao.getDownloadByWorkId(id.toString())?.let { record ->
                downloadDao.update(record.copy(status = com.example.pulsar.data.model.DownloadStatus.DOWNLOADING))
            }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val pulsarDir = File(downloadsDir, "Pulsar").apply { if (!exists()) mkdirs() }

            val request = YoutubeDLRequest(url).apply {
                if (isAudio) {
                    addOption("-f", formatId)
                    addOption("--extract-audio")
                    addOption("--audio-format", "mp3")
                } else {
                    addOption("-f", "$formatId+bestaudio/best")
                    addOption("--merge-output-format", "mp4")
                }

                // Use a generic name that we can predict
                addOption("-o", "${pulsarDir.absolutePath}/$id.%(ext)s")
                addOption("--restrict-filenames")

                // ---------------------------------------------------------
                // Apply download settings
                // ---------------------------------------------------------
                // Max Concurrent Chunks
                addOption("-N", maxConcurrent.toString())

                // Use aria2c Engine
                if (useAria) {
                    addOption("--downloader", "aria2c")
                    addOption("--downloader-args", "aria2c:-x $maxConcurrent")
                }

                // Inject Sanitized Custom Flags
                if (customFlags.isNotBlank()) {
                    sanitizeFlags(customFlags).forEach { flag ->
                        addOption(flag)
                    }
                }
                // ---------------------------------------------------------
            }

            val response = YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, speed ->
                setProgressAsync(
                    workDataOf(
                        "PROGRESS" to progress,
                        "ETA" to etaInSeconds,
                        "SPEED" to speed
                    )
                )

                // Sync progress with DB
                val etaString = if (etaInSeconds > 0) {
                    "${etaInSeconds / 60}:${String.format(java.util.Locale.getDefault(), "%02d", etaInSeconds % 60)} left"
                } else ""
                val speedMatch = Regex("([0-9.]+\\s*[a-zA-Z]+/s)").find(speed)?.value ?: ""

                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    downloadDao.getDownloadByWorkId(id.toString())?.let { record ->
                        // ONLY update if we haven't been cancelled or finished elsewhere
                        if (record.status == com.example.pulsar.data.model.DownloadStatus.DOWNLOADING ||
                            record.status == com.example.pulsar.data.model.DownloadStatus.QUEUED) {
                            downloadDao.update(record.copy(
                                progress = progress.toInt(),
                                speed = speedMatch,
                                eta = etaString,
                                status = com.example.pulsar.data.model.DownloadStatus.DOWNLOADING
                            ))
                        }
                    }
                }
            }

            // Check Result
            if (response.exitCode == 0) {
                // Find the downloaded file by searching the directory for the known ID
                val downloadedFile = pulsarDir.listFiles()?.find { file ->
                    file.name.startsWith("$id.") && !file.name.endsWith(".part") && !file.name.endsWith(".ytdl")
                }

                if (downloadedFile != null && downloadedFile.exists()) {
                    // Now, rename it to the *actual* title the user expects
                    val extension = downloadedFile.extension
                    // Sanitize the title to be a valid filename (allowing spaces and non-English characters)
                    val sanitizedTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    var finalFileName = "$sanitizedTitle.$extension"
                    var finalFile = File(pulsarDir, finalFileName)

                    // Handle duplicate file names
                    var counter = 1
                    while (finalFile.exists()) {
                        finalFileName = "$sanitizedTitle ($counter).$extension"
                        finalFile = File(pulsarDir, finalFileName)
                        counter++
                    }

                    if (downloadedFile.renameTo(finalFile)) {
                        // Update DB to COMPLETED
                        downloadDao.getDownloadByWorkId(id.toString())?.let { record ->
                            downloadDao.update(record.copy(
                                status = com.example.pulsar.data.model.DownloadStatus.COMPLETED,
                                progress = 100,
                                filePath = finalFile.absolutePath
                            ))
                        }
                        Result.success(workDataOf("FILE_PATH" to finalFile.absolutePath))
                    } else {
                        // Update DB to FAILED since rename failed
                        downloadDao.getDownloadByWorkId(id.toString())?.let { record ->
                            downloadDao.update(record.copy(status = com.example.pulsar.data.model.DownloadStatus.FAILED))
                        }
                        Result.failure(workDataOf("ERROR" to "Failed to rename file to $finalFileName"))
                    }
                } else {
                    // Update DB to FAILED since file wasn't found
                    downloadDao.getDownloadByWorkId(id.toString())?.let { record ->
                        downloadDao.update(record.copy(status = com.example.pulsar.data.model.DownloadStatus.FAILED))
                    }
                    Result.failure(workDataOf("ERROR" to "Could not find downloaded file in directory for id: $id"))
                }
            } else {
                // Update DB to FAILED
                downloadDao.getDownloadByWorkId(id.toString())?.let { record ->
                    downloadDao.update(record.copy(status = com.example.pulsar.data.model.DownloadStatus.FAILED))
                }
                Result.failure(workDataOf("ERROR" to response.err))
            }

        } catch (e: CancellationException) {
            // Update DB to CANCELLED
            downloadDao.getDownloadByWorkId(id.toString())?.let { record ->
                downloadDao.update(record.copy(status = com.example.pulsar.data.model.DownloadStatus.CANCELLED))
            }
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            // Update DB to FAILED
            downloadDao.getDownloadByWorkId(id.toString())?.let { record ->
                downloadDao.update(record.copy(status = com.example.pulsar.data.model.DownloadStatus.FAILED))
            }
            Result.failure(workDataOf("ERROR" to e.localizedMessage))
        } finally {
            try {
                YoutubeDL.getInstance().destroyProcessById(processId)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun sanitizeFlags(flags: String): List<String> {
        val dangerousKeywords = listOf(
            "--exec", "--post-processor-args", "--downloader-args",
            "--external-downloader-args", "--print-to-file", "--load-pages",
            "--print-json", "--get-description", "--get-id"
        )

        return flags.split(" ")
            .filter { it.isNotBlank() }
            .map { it.trim() }
            .filter { flag ->
                dangerousKeywords.none { keyword -> flag.startsWith(keyword, ignoreCase = true) }
            }
    }

    private fun copyToPublicDownloads(sourceFile: File) {
        val resolver = applicationContext.contentResolver
        val fileName = sourceFile.name
        val isVideo = fileName.endsWith(".mp4", ignoreCase = true) || fileName.endsWith(".mkv", ignoreCase = true) || fileName.endsWith(".webm", ignoreCase = true)
        val mimeType = if (isVideo) "video/mp4" else "audio/mpeg"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (API 29+): Use MediaStore to safely write to the public Downloads folder
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Pulsar")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val destUri = resolver.insert(collection, contentValues)
                ?: throw Exception("Failed to create MediaStore entry")

            resolver.openOutputStream(destUri)?.use { outputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(destUri, contentValues, null, null)

        } else {
            // Android 9 and below: Fallback to direct file copy (Requires WRITE_EXTERNAL_STORAGE permission)
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val pulsarDir = File(downloadsDir, "Pulsar").apply { if (!exists()) mkdirs() }
            val destFile = File(pulsarDir, fileName)
            sourceFile.copyTo(destFile, overwrite = true)
        }
    }
}