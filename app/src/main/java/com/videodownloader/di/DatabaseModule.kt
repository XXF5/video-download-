package com.videodownloader.di

import android.content.Context
import androidx.room.Room
import com.videodownloader.data.database.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "video_downloader_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDownloadDao(database: AppDatabase): DownloadDao {
        return database.downloadDao()
    }

    @Provides
    fun provideBrowserHistoryDao(database: AppDatabase): BrowserHistoryDao {
        return database.browserHistoryDao()
    }

    @Provides
    fun provideBookmarksDao(database: AppDatabase): BookmarksDao {
        return database.bookmarksDao()
    }
}
