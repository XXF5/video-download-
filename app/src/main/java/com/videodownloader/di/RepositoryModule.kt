package com.videodownloader.di

import com.videodownloader.data.database.*
import com.videodownloader.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideDownloadRepository(
        downloadDao: DownloadDao
    ): DownloadRepository {
        return DownloadRepositoryImpl(downloadDao)
    }

    @Provides
    @Singleton
    fun provideBrowserRepository(
        historyDao: BrowserHistoryDao,
        bookmarksDao: BookmarksDao
    ): BrowserRepository {
        return BrowserRepositoryImpl(historyDao, bookmarksDao)
    }
}
