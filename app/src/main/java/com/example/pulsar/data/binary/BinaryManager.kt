package com.example.pulsar.data.binary

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the extraction and configuration of binary executables (like yt-dlp/aria2c).
 */
@Singleton
class BinaryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Android's package manager automatically extracts .so files to this directory
    // and grants them executable permissions during app installation.
    private val nativeLibDir = context.applicationInfo.nativeLibraryDir

    suspend fun extractBinariesIfNeeded() {
        // Extraction is handled automatically by the library.
        // The OS handles it safely and legally.
    }

    fun getYtDlpPath(): String {
        // Point directly to the extracted .so file
        return File(nativeLibDir, "libyt-dlp.so").absolutePath
    }

    fun getAria2cPath(): String? {
        val file = File(nativeLibDir, "libaria2c.so")
        return if (file.exists() && file.canExecute()) file.absolutePath else null
    }
}