package com.example.pulsar.di

import android.content.Context
import androidx.room.Room
import com.example.pulsar.data.db.DownloadDao
import com.example.pulsar.data.db.PulsarDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing database and DAO dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PulsarDatabase {
        return Room.databaseBuilder(
            context,
            PulsarDatabase::class.java,
            "pulsar_database"
        ).fallbackToDestructiveMigration().build()
    }

    // Provides the DAO whenever a ViewModel or Worker asks for it
    @Provides
    @Singleton
    fun provideDownloadDao(database: PulsarDatabase): DownloadDao {
        return database.downloadDao()
    }
}