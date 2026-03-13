package com.videodownloader.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videodownloader.core.browser.BrowserManager
import com.videodownloader.data.models.BrowserTab
import com.videodownloader.data.models.DetectedVideo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val browserManager: BrowserManager
) : ViewModel() {
    
    val tabs: StateFlow<List<BrowserTab>> = browserManager.allTabs
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val tabsCount: StateFlow<Int> = browserManager.tabsCount
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
    
    fun createTab(url: String) {
        viewModelScope.launch {
            browserManager.createTab(url)
        }
    }
    
    fun closeTab(tabId: Long) {
        viewModelScope.launch {
            browserManager.closeTab(tabId)
        }
    }
    
    fun switchToTab(tabId: Long) {
        viewModelScope.launch {
            browserManager.switchToTab(tabId)
        }
    }
}
