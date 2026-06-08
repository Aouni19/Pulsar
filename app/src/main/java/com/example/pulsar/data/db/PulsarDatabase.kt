package com.example.pulsar.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.pulsar.data.model.DownloadRecord

/**
 * The main Room database for the Pulsar application.
 */
@Database(entities = [DownloadRecord::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PulsarDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}