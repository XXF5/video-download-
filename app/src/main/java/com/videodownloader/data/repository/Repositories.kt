package com.videodownloader.data.repository

import com.videodownloader.data.database.*
import com.videodownloader.data.models.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

interface DownloadRepository {
    fun getAllDownloads(): Flow<List<DownloadTask>>
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadTask>>
    suspend fun getDownloadById(id: Long): DownloadTask?
    suspend fun getActiveDownloads(): List<DownloadTask>
    suspend fun getPausedDownloads(): List<DownloadTask>
    fun getActiveDownloadsCount(): Flow<Int>
    suspend fun insertDownload(download: DownloadTask): Long
    suspend fun updateDownload(download: DownloadTask)
    suspend fun deleteDownload(download: DownloadTask)
    suspend fun deleteDownloadById(id: Long)
    suspend fun deleteCompletedDownloads()
    suspend fun deleteFailedDownloads()
    suspend fun deleteAllDownloads()
    suspend fun updateProgress(id: Long, downloadedBytes: Long, speed: Long, timeRemaining: Long)
    suspend fun updateStatus(id: Long, status: DownloadStatus, error: String)
}

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao
) : DownloadRepository {
    override fun getAllDownloads() = downloadDao.getAllDownloads()
    override fun getDownloadsByStatus(status: DownloadStatus) = downloadDao.getDownloadsByStatus(status)
    override suspend fun getDownloadById(id: Long) = downloadDao.getDownloadById(id)
    override suspend fun getActiveDownloads() = downloadDao.getActiveDownloads()
    override suspend fun getPausedDownloads() = downloadDao.getPausedDownloads()
    override fun getActiveDownloadsCount() = downloadDao.getActiveDownloadsCount()
    override suspend fun insertDownload(download: DownloadTask) = downloadDao.insertDownload(download)
    override suspend fun updateDownload(download: DownloadTask) = downloadDao.updateDownload(download)
    override suspend fun deleteDownload(download: DownloadTask) = downloadDao.deleteDownload(download)
    override suspend fun deleteDownloadById(id: Long) = downloadDao.deleteDownloadById(id)
    override suspend fun deleteCompletedDownloads() = downloadDao.deleteCompletedDownloads()
    override suspend fun deleteFailedDownloads() = downloadDao.deleteFailedDownloads()
    override suspend fun deleteAllDownloads() = downloadDao.deleteAllDownloads()
    override suspend fun updateProgress(id: Long, downloadedBytes: Long, speed: Long, timeRemaining: Long) = 
        downloadDao.updateProgress(id, downloadedBytes, speed, timeRemaining)
    override suspend fun updateStatus(id: Long, status: DownloadStatus, error: String) = 
        downloadDao.updateStatus(id, status, error)
}

interface BrowserRepository {
    fun getAllHistory(): Flow<List<BrowserHistoryItem>>
    fun getRecentHistory(limit: Int): Flow<List<BrowserHistoryItem>>
    fun searchHistory(query: String): Flow<List<BrowserHistoryItem>>
    fun getUniqueHistory(limit: Int): Flow<List<BrowserHistoryItem>>
    suspend fun insertHistory(item: BrowserHistoryItem)
    suspend fun deleteHistory(item: BrowserHistoryItem)
    suspend fun deleteHistoryById(id: Long)
    suspend fun clearHistory()
    fun getAllBookmarks(): Flow<List<BookmarkItem>>
    suspend fun getBookmarkByUrl(url: String): BookmarkItem?
    suspend fun insertBookmark(item: BookmarkItem)
    suspend fun deleteBookmark(item: BookmarkItem)
    suspend fun deleteBookmarkByUrl(url: String)
    
    // Tabs
    fun getAllTabs(): Flow<List<BrowserTab>>
    suspend fun createTab(url: String, title: String): BrowserTab
    suspend fun updateTabUrl(id: Long, url: String, title: String)
    suspend fun setActiveTab(id: Long)
    suspend fun closeTab(id: Long)
    suspend fun getActiveTab(): BrowserTab?
    suspend fun getTabById(id: Long): BrowserTab?
    suspend fun addToHistory(url: String, title: String)
}

@Singleton
class BrowserRepositoryImpl @Inject constructor(
    private val historyDao: BrowserHistoryDao,
    private val bookmarksDao: BookmarksDao,
    private val tabsDao: BrowserTabsDao
) : BrowserRepository {
    override fun getAllHistory() = historyDao.getAllHistory()
    override fun getRecentHistory(limit: Int) = historyDao.getRecentHistory(limit)
    override fun searchHistory(query: String) = historyDao.searchHistory(query)
    override fun getUniqueHistory(limit: Int) = historyDao.getUniqueHistory(limit)
    override suspend fun insertHistory(item: BrowserHistoryItem) = historyDao.insertHistory(item)
    override suspend fun deleteHistory(item: BrowserHistoryItem) = historyDao.deleteHistory(item)
    override suspend fun deleteHistoryById(id: Long) = historyDao.deleteHistoryById(id)
    override suspend fun clearHistory() = historyDao.clearHistory()
    override fun getAllBookmarks() = bookmarksDao.getAllBookmarks()
    override suspend fun getBookmarkByUrl(url: String) = bookmarksDao.getBookmarkByUrl(url)
    override suspend fun insertBookmark(item: BookmarkItem) = bookmarksDao.insertBookmark(item)
    override suspend fun deleteBookmark(item: BookmarkItem) = bookmarksDao.deleteBookmark(item)
    override suspend fun deleteBookmarkByUrl(url: String) = bookmarksDao.deleteBookmarkByUrl(url)
    
    override fun getAllTabs() = tabsDao.getAllTabs()
    override suspend fun createTab(url: String, title: String): BrowserTab {
        val position = tabsDao.getMaxPosition() ?: 0
        val tab = BrowserTab(url = url, title = title, position = position + 1, isActive = true)
        val id = tabsDao.insertTab(tab)
        tabsDao.getAllTabsList().filter { it.id != id }.forEach { 
            tabsDao.updateTab(it.copy(isActive = false))
        }
        return tab.copy(id = id)
    }
    override suspend fun updateTabUrl(id: Long, url: String, title: String) {
        tabsDao.getTabById(id)?.let { 
            tabsDao.updateTab(it.copy(url = url, title = title, lastVisited = System.currentTimeMillis()))
        }
    }
    override suspend fun setActiveTab(id: Long) = tabsDao.setActiveTab(id)
    override suspend fun closeTab(id: Long) {
        val tab = tabsDao.getTabById(id) ?: return
        tabsDao.deleteTab(tab)
        if (tab.isActive) {
            tabsDao.getAllTabsList().firstOrNull()?.let { tabsDao.setActiveTab(it.id) }
        }
    }
    override suspend fun getActiveTab() = tabsDao.getActiveTab()
    override suspend fun getTabById(id: Long) = tabsDao.getTabById(id)
    override suspend fun addToHistory(url: String, title: String) {
        historyDao.insertHistory(BrowserHistoryItem(url = url, title = title))
    }
}
