package com.example.pulsar.data.db

import androidx.room.TypeConverter
import com.example.pulsar.data.model.DownloadStatus

/**
 * Room database type converters for custom data types.
 */
class Converters {
    @TypeConverter
    fun toDownloadStatus(value: String) = enumValueOf<DownloadStatus>(value)

    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus) = value.name
}