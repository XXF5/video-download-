package com.videodownloader.core.browser

import android.content.Context
import com.videodownloader.data.database.*
import com.videodownloader.data.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrowserManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tabsDao: BrowserTabsDao,
    private val historyDao: BrowserHistoryDao,
    private val bookmarksDao: BookmarksDao
) {
    val allTabs: Flow<List<BrowserTab>> = tabsDao.getAllTabs()
    val tabsCount: Flow<Int> = tabsDao.getTabsCount()
    val allHistory: Flow<List<BrowserHistoryItem>> = historyDao.getAllHistory()
    val recentHistory: Flow<List<BrowserHistoryItem>> = historyDao.getRecentHistory(50)
    val uniqueHistory: Flow<List<BrowserHistoryItem>> = historyDao.getUniqueHistory(20)
    val allBookmarks: Flow<List<BookmarkItem>> = bookmarksDao.getAllBookmarks()

    suspend fun createTab(url: String, title: String = ""): BrowserTab {
        val position = tabsDao.getMaxPosition() ?: 0
        val tab = BrowserTab(url = url, title = title.ifEmpty { "New Tab" }, position = position + 1, isActive = true)
        val id = tabsDao.insertTab(tab)
        tabsDao.getAllTabsList().filter { it.id != id }.forEach { tabsDao.updateTab(it.copy(isActive = false)) }
        return tab.copy(id = id)
    }

    suspend fun getActiveTab(): BrowserTab? = tabsDao.getActiveTab()
    suspend fun getTabById(id: Long): BrowserTab? = tabsDao.getTabById(id)
    suspend fun switchToTab(tabId: Long) = tabsDao.setActiveTab(tabId)

    suspend fun updateTabUrl(tabId: Long, url: String, title: String = "") {
        tabsDao.getTabById(tabId)?.let { tab ->
            tabsDao.updateTab(tab.copy(url = url, title = title.ifEmpty { tab.title }, lastVisited = System.currentTimeMillis()))
        }
    }

    suspend fun closeTab(tabId: Long) {
        val tab = tabsDao.getTabById(tabId) ?: return
        tabsDao.deleteTab(tab)
        if (tab.isActive) tabsDao.getAllTabsList().firstOrNull()?.let { tabsDao.setActiveTab(it.id) }
    }

    suspend fun closeAllTabs() = tabsDao.deleteAllTabs()
    suspend fun restoreSession(): List<BrowserTab> = tabsDao.getAllTabsList()

    suspend fun addToHistory(url: String, title: String = "", favicon: String = "") {
        historyDao.insertHistory(BrowserHistoryItem(url = url, title = title, favicon = favicon))
    }

    fun searchHistory(query: String): Flow<List<BrowserHistoryItem>> = historyDao.searchHistory(query)
    suspend fun clearHistory() = historyDao.clearHistory()
    suspend fun deleteHistoryItem(id: Long) = historyDao.deleteHistoryById(id)

    suspend fun addBookmark(url: String, title: String = "", favicon: String = "") {
        bookmarksDao.insertBookmark(BookmarkItem(url = url, title = title, favicon = favicon))
    }

    suspend fun isBookmarked(url: String): Boolean = bookmarksDao.getBookmarkByUrl(url) != null
    suspend fun removeBookmark(url: String) = bookmarksDao.deleteBookmarkByUrl(url)

    suspend fun toggleBookmark(url: String, title: String = "", favicon: String = ""): Boolean {
        return if (isBookmarked(url)) { removeBookmark(url); false }
        else { addBookmark(url, title, favicon); true }
    }
}
