package com.videodownloader.data.database

import androidx.room.*
import com.videodownloader.data.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserTabsDao {
    @Query("SELECT * FROM browser_tabs ORDER BY position ASC")
    fun getAllTabs(): Flow<List<BrowserTab>>
    
    @Query("SELECT * FROM browser_tabs ORDER BY position ASC")
    suspend fun getAllTabsList(): List<BrowserTab>
    
    @Query("SELECT * FROM browser_tabs WHERE id = :id")
    suspend fun getTabById(id: Long): BrowserTab?
    
    @Query("SELECT * FROM browser_tabs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTab(): BrowserTab?
    
    @Query("SELECT COUNT(*) FROM browser_tabs")
    fun getTabsCount(): Flow<Int>
    
    @Query("SELECT MAX(position) FROM browser_tabs")
    suspend fun getMaxPosition(): Int?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: BrowserTab): Long
    
    @Update
    suspend fun updateTab(tab: BrowserTab)
    
    @Delete
    suspend fun deleteTab(tab: BrowserTab)
    
    @Query("DELETE FROM browser_tabs")
    suspend fun deleteAllTabs()
    
    @Query("UPDATE browser_tabs SET isActive = 0")
    suspend fun deactivateAllTabs()
    
    @Transaction
    suspend fun setActiveTab(id: Long) {
        deactivateAllTabs()
        getTabById(id)?.let { updateTab(it.copy(isActive = true, lastVisited = System.currentTimeMillis())) }
    }
}

@Dao
interface BrowserHistoryDao {
    @Query("SELECT * FROM browser_history ORDER BY visitedAt DESC")
    fun getAllHistory(): Flow<List<BrowserHistoryItem>>
    
    @Query("SELECT * FROM browser_history ORDER BY visitedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<BrowserHistoryItem>>
    
    @Query("SELECT * FROM browser_history WHERE url LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY visitedAt DESC")
    fun searchHistory(query: String): Flow<List<BrowserHistoryItem>>
    
    @Query("SELECT *, MAX(visitedAt) as maxVisited FROM browser_history GROUP BY url ORDER BY maxVisited DESC LIMIT :limit")
    fun getUniqueHistory(limit: Int): Flow<List<BrowserHistoryItem>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: BrowserHistoryItem)
    
    @Query("DELETE FROM browser_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)
    
    @Query("DELETE FROM browser_history")
    suspend fun clearHistory()
}

@Dao
interface BookmarksDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkItem>>
    
    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun getBookmarkByUrl(url: String): BookmarkItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(item: BookmarkItem)
    
    @Delete
    suspend fun deleteBookmark(item: BookmarkItem)
    
    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadTask>>
    
    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadTask?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(task: DownloadTask): Long
    
    @Update
    suspend fun updateDownload(task: DownloadTask)
    
    @Delete
    suspend fun deleteDownload(task: DownloadTask)
    
    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: Long)
}

@Dao
interface StreamingSessionDao {
    @Query("SELECT * FROM streaming_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<StreamingSession>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StreamingSession): Long
    
    @Update
    suspend fun updateSession(session: StreamingSession)
    
    @Query("DELETE FROM streaming_sessions")
    suspend fun clearAllSessions()
}

@Database(
    entities = [BrowserTab::class, BrowserHistoryItem::class, BookmarkItem::class, DownloadTask::class, StreamingSession::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun browserTabsDao(): BrowserTabsDao
    abstract fun browserHistoryDao(): BrowserHistoryDao
    abstract fun bookmarksDao(): BookmarksDao
    abstract fun downloadDao(): DownloadDao
    abstract fun streamingSessionDao(): StreamingSessionDao
}
