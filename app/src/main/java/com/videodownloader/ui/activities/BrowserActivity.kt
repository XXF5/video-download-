package com.videodownloader.ui.activities

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.videodownloader.R
import com.videodownloader.core.browser.BrowserManager
import com.videodownloader.core.extractor.VideoExtractor
import com.videodownloader.data.models.BrowserTab
import com.videodownloader.data.models.DetectedVideo
import com.videodownloader.databinding.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Advanced Browser Activity with Full Browsing Support
 * Features:
 * - Unlimited Tabs
 * - Video Detection
 * - History & Bookmarks
 * - Streaming Support
 * - Built-in Search
 * - Home Page with Quick Links
 */
@AndroidEntryPoint
class BrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowserBinding
    private val viewModel: BrowserViewModel by viewModels()
    
    @Inject
    lateinit var videoExtractor: VideoExtractor
    
    @Inject
    lateinit var browserManager: BrowserManager

    private val webViewContainer = mutableMapOf<Long, WebView>()
    private var activeWebView: WebView? = null
    private var activeTabId: Long = -1
    private var isHomePage = true
    
    private val detectedVideos = mutableListOf<DetectedVideo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupObservers()
        setupBackHandler()
        showHomePage()
        
        // Restore or create initial tab
        lifecycleScope.launch {
            val existingTabs = browserManager.restoreSession()
            if (existingTabs.isEmpty()) {
                // Start with home page, no tab needed yet
            } else {
                restoreTabs(existingTabs)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupUI() {
        // URL Bar - focus to search
        binding.etUrl.setOnClickListener {
            binding.etUrl.selectAll()
        }
        
        binding.etUrl.setOnEditorActionListener { _, _, _ ->
            val url = binding.etUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                loadUrl(url)
            }
            true
        }

        // Voice search button (placeholder)
        binding.btnVoiceSearch.setOnClickListener {
            Toast.makeText(this, "Voice search coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Navigation buttons
        binding.btnBack.setOnClickListener { 
            if (isHomePage) {
                showExitConfirmation()
            } else {
                activeWebView?.goBack() 
            }
        }
        binding.btnForward.setOnClickListener { activeWebView?.goForward() }
        binding.btnRefresh.setOnClickListener { activeWebView?.reload() }
        binding.btnHome.setOnClickListener { showHomePage() }

        // Tab management
        binding.btnAddTab.setOnClickListener { createNewTab("about:blank") }
        binding.btnTabs.setOnClickListener { showTabsDialog() }
        
        // Menu
        binding.btnMenu.setOnClickListener { showMenuDialog() }
        
        // Download detected videos
        binding.btnDownload.setOnClickListener { showDetectedVideos() }
        binding.btnDetectedVideos.setOnClickListener { showDetectedVideos() }
        
        // Stream button
        binding.btnStream.setOnClickListener { streamCurrentVideo() }
        
        // Setup Home Page
        setupHomePage()
    }

    private fun setupHomePage() {
        // Quick Links Adapter
        val quickLinks = listOf(
            QuickLink("YouTube", "https://youtube.com", R.drawable.ic_youtube),
            QuickLink("TikTok", "https://tiktok.com", R.drawable.ic_tiktok),
            QuickLink("Instagram", "https://instagram.com", R.drawable.ic_instagram),
            QuickLink("Facebook", "https://facebook.com", R.drawable.ic_facebook),
            QuickLink("Twitter", "https://twitter.com", R.drawable.ic_twitter),
            QuickLink("Vimeo", "https://vimeo.com", R.drawable.ic_vimeo),
        )
        
        val quickLinksAdapter = QuickLinksAdapter { link ->
            loadUrl(link.url)
        }
        
        binding.rvQuickLinks.apply {
            layoutManager = GridLayoutManager(this@BrowserActivity, 4)
            adapter = quickLinksAdapter
        }
        quickLinksAdapter.submitList(quickLinks)
        
        // Recent History
        lifecycleScope.launch {
            browserManager.uniqueHistory.collect { history ->
                if (history.isNotEmpty()) {
                    val historyAdapter = RecentHistoryAdapter { item ->
                        loadUrl(item.url)
                    }
                    binding.rvRecentHistory.apply {
                        layoutManager = LinearLayoutManager(this@BrowserActivity)
                        adapter = historyAdapter
                    }
                    historyAdapter.submitList(history.take(10))
                    binding.recentHistorySection.isVisible = true
                } else {
                    binding.recentHistorySection.isVisible = false
                }
            }
        }
        
        // Search bar focus
        binding.searchBar.setOnClickListener {
            binding.etUrl.requestFocus()
            binding.etUrl.selectAll()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            browserManager.allTabs.collect { tabs ->
                binding.tvTabsCount.text = tabs.size.toString()
                binding.tvTabsCount.isVisible = tabs.size > 0
            }
        }
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isHomePage) {
                    showExitConfirmation()
                } else if (activeWebView?.canGoBack() == true) {
                    activeWebView?.goBack()
                } else {
                    showHomePage()
                }
            }
        })
    }

    private fun showHomePage() {
        isHomePage = true
        binding.homePageContainer.isVisible = true
        binding.webViewContainer.isVisible = false
        
        // Update UI
        binding.etUrl.setText("")
        binding.etUrl.hint = "Search or enter URL"
        binding.progressBar.isVisible = false
        binding.btnBack.isEnabled = false
        binding.btnForward.isEnabled = false
        
        // Hide video detection UI
        binding.bottomVideoActions.isVisible = false
        detectedVideos.clear()
        updateDetectionBadge()
    }

    private fun hideHomePage() {
        isHomePage = false
        binding.homePageContainer.isVisible = false
        binding.webViewContainer.isVisible = true
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createNewTab(url: String) {
        hideHomePage()
        
        lifecycleScope.launch {
            val tab = browserManager.createTab(url)
            val webView = createWebView()
            
            webViewContainer[tab.id] = webView
            binding.webViewContainer.addView(webView)
            
            switchToTab(tab)
            
            if (url != "about:blank" && url.isNotEmpty()) {
                webView.loadUrl(url)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
        return WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                allowFileAccess = true
                allowContentAccess = true
                mediaPlaybackRequiresUserGesture = false
                blockNetworkImage = false
                loadsImagesAutomatically = true
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = false
                }
            }
            
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    binding.progressBar.isVisible = true
                    binding.progressBar.progress = 0
                    binding.etUrl.setText(url)
                    detectedVideos.clear()
                    updateDetectionBadge()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.isVisible = false
                    binding.btnBack.isEnabled = view?.canGoBack() == true
                    binding.btnForward.isEnabled = view?.canGoForward() == true
                    
                    lifecycleScope.launch {
                        browserManager.updateTabUrl(activeTabId, url ?: "", view?.title ?: "")
                        browserManager.addToHistory(url ?: "", view?.title ?: "")
                    }
                    
                    injectVideoDetectionScript()
                    runDeepVideoDetection()
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    request?.url?.toString()?.let { url ->
                        if (isVideoUrl(url) && detectedVideos.none { it.url == url }) {
                            runOnUiThread {
                                detectedVideos.add(DetectedVideo(
                                    url = url,
                                    title = view?.title ?: "",
                                    source = "Browser"
                                ))
                                updateDetectionBadge()
                                showVideoDetectedToast()
                            }
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    request?.url?.let { url ->
                        when {
                            url.scheme == "intent" -> {
                                try {
                                    val intent = Intent.parseUri(url.toString(), Intent.URI_INTENT_SCHEME)
                                    startActivity(intent)
                                    return true
                                } catch (e: Exception) { }
                            }
                            url.scheme == "market" -> {
                                try {
                                    startActivity(Intent(Intent.ACTION_VIEW, url))
                                    return true
                                } catch (e: Exception) { }
                            }
                            url.scheme == "tel" -> {
                                startActivity(Intent(Intent.ACTION_DIAL, url))
                                return true
                            }
                            url.scheme == "mailto" -> {
                                startActivity(Intent(Intent.ACTION_SENDTO, url))
                                return true
                            }
                        }
                    }
                    return false
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    binding.progressBar.progress = newProgress
                }
                
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    lifecycleScope.launch {
                        browserManager.updateTabUrl(activeTabId, view?.url ?: "", title ?: "")
                    }
                }
                
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    return true
                }
            }
            
            setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                handleDownload(url, userAgent, contentDisposition, mimeType, contentLength)
            }
        }
    }

    private fun switchToTab(tab: BrowserTab) {
        hideHomePage()
        activeTabId = tab.id
        
        webViewContainer.values.forEach { it.isVisible = false }
        
        webViewContainer[tab.id]?.let { webView ->
            webView.isVisible = true
            activeWebView = webView
            binding.etUrl.setText(webView.url ?: tab.url)
        }
        
        lifecycleScope.launch {
            browserManager.switchToTab(tab.id)
        }
    }

    private fun restoreTabs(tabs: List<BrowserTab>) {
        tabs.forEach { tab ->
            val webView = createWebView()
            webViewContainer[tab.id] = webView
            binding.webViewContainer.addView(webView)
            webView.loadUrl(tab.url)
        }
        
        tabs.find { it.isActive }?.let { switchToTab(it) }
            ?: tabs.firstOrNull()?.let { switchToTab(it) }
    }

    private fun loadUrl(url: String) {
        hideHomePage()
        
        var finalUrl = url.trim()
        if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
            if (finalUrl.contains(".") && !finalUrl.contains(" ")) {
                finalUrl = "https://$finalUrl"
            } else {
                finalUrl = "https://www.google.com/search?q=${java.net.URLEncoder.encode(finalUrl, "UTF-8")}"
            }
        }
        
        if (activeWebView == null) {
            createNewTab(finalUrl)
        } else {
            activeWebView?.loadUrl(finalUrl)
        }
    }

    private fun injectVideoDetectionScript() {
        val script = """
            (function() {
                var videos = [];
                
                document.querySelectorAll('video').forEach(function(v) {
                    if (v.src) videos.push({url: v.src, type: 'video'});
                    v.querySelectorAll('source').forEach(function(s) {
                        if (s.src) videos.push({url: s.src, type: 'source'});
                    });
                });
                
                document.querySelectorAll('script').forEach(function(script) {
                    var content = script.textContent || '';
                    var patterns = [
                        /["'](https?:\/\/[^"']+\.m3u8[^"']*)["']/gi,
                        /["'](https?:\/\/[^"']+\.mpd[^"']*)["']/gi,
                        /["'](https?:\/\/[^"']+\.mp4[^"']*)["']/gi
                    ];
                    patterns.forEach(function(p) {
                        var match;
                        while ((match = p.exec(content)) !== null) {
                            videos.push({url: match[1], type: 'stream'});
                        }
                    });
                });
                
                if (videos.length > 0) {
                    window.VideoDetector?.onVideosDetected(JSON.stringify(videos));
                }
                
                return videos.length;
            })();
        """.trimIndent()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activeWebView?.evaluateJavascript(script) { }
        }
    }

    private fun runDeepVideoDetection() {
        activeWebView?.url?.let { url ->
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val videos = videoExtractor.detectVideosDeep(url)
                    withContext(Dispatchers.Main) {
                        videos.forEach { video ->
                            if (detectedVideos.none { it.url == video.url }) {
                                detectedVideos.add(video)
                            }
                        }
                        updateDetectionBadge()
                    }
                } catch (e: Exception) {
                    // Silently fail
                }
            }
        }
    }

    private fun showTabsDialog() {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogTabsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        
        val adapter = TabsAdapter(
            onItemClick = { tab ->
                switchToTab(tab)
                dialog.dismiss()
            },
            onCloseClick = { tab ->
                closeTab(tab.id)
                if (webViewContainer.isEmpty()) {
                    dialog.dismiss()
                    showHomePage()
                }
            }
        )
        
        dialogBinding.rvTabs.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvTabs.adapter = adapter
        
        lifecycleScope.launch {
            browserManager.allTabs.collect { tabs ->
                adapter.submitList(tabs)
            }
        }
        
        dialogBinding.btnAddTab.setOnClickListener {
            showHomePage()
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun showMenuDialog() {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogBrowserMenuBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        
        dialogBinding.btnHistory.setOnClickListener {
            showHistoryDialog()
            dialog.dismiss()
        }
        
        dialogBinding.btnBookmarks.setOnClickListener {
            showBookmarksDialog()
            dialog.dismiss()
        }
        
        dialogBinding.btnShare.setOnClickListener {
            shareCurrentPage()
            dialog.dismiss()
        }
        
        dialogBinding.btnRefresh.setOnClickListener {
            activeWebView?.reload()
            dialog.dismiss()
        }
        
        dialogBinding.btnDesktopMode.setOnCheckedChangeListener { _, isChecked ->
            activeWebView?.settings?.userAgentString = if (isChecked) {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            } else {
                null
            }
            activeWebView?.reload()
        }
        
        dialog.show()
    }

    private fun showHistoryDialog() {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogHistoryBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        
        val adapter = HistoryAdapter(
            onItemClick = { item ->
                loadUrl(item.url)
                dialog.dismiss()
            },
            onDeleteClick = { item ->
                lifecycleScope.launch {
                    browserManager.deleteHistoryItem(item.id)
                }
            }
        )
        
        dialogBinding.rvHistory.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvHistory.adapter = adapter
        
        lifecycleScope.launch {
            browserManager.recentHistory.collect { history ->
                adapter.submitList(history)
            }
        }
        
        dialogBinding.btnClearHistory.setOnClickListener {
            lifecycleScope.launch {
                browserManager.clearHistory()
                Toast.makeText(this@BrowserActivity, R.string.history_cleared, Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.show()
    }

    private fun showBookmarksDialog() {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogBookmarksBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        
        val adapter = BookmarksAdapter(
            onItemClick = { item ->
                loadUrl(item.url)
                dialog.dismiss()
            },
            onDeleteClick = { item ->
                lifecycleScope.launch {
                    browserManager.removeBookmark(item.url)
                }
            }
        )
        
        dialogBinding.rvBookmarks.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvBookmarks.adapter = adapter
        
        lifecycleScope.launch {
            browserManager.allBookmarks.collect { bookmarks ->
                adapter.submitList(bookmarks)
            }
        }
        
        dialogBinding.btnAddBookmark.setOnClickListener {
            lifecycleScope.launch {
                val isBookmarked = browserManager.toggleBookmark(
                    activeWebView?.url ?: "",
                    activeWebView?.title ?: ""
                )
                Toast.makeText(
                    this@BrowserActivity,
                    if (isBookmarked) R.string.bookmark_added else R.string.bookmark_removed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        dialog.show()
    }

    private fun showDetectedVideos() {
        if (detectedVideos.isEmpty()) {
            Toast.makeText(this, R.string.no_videos_detected, Toast.LENGTH_SHORT).show()
            return
        }
        
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogDetectedVideosBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        
        val adapter = DetectedVideosAdapter(
            onDownloadClick = { video ->
                startDownload(video)
                dialog.dismiss()
            },
            onStreamClick = { video ->
                streamVideo(video)
                dialog.dismiss()
            }
        )
        
        dialogBinding.rvVideos.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvVideos.adapter = adapter
        adapter.submitList(detectedVideos)
        
        dialog.show()
    }

    private fun streamCurrentVideo() {
        if (detectedVideos.isEmpty()) {
            Toast.makeText(this, R.string.no_videos_detected, Toast.LENGTH_SHORT).show()
            return
        }
        streamVideo(detectedVideos.first())
    }

    private fun streamVideo(video: DetectedVideo) {
        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
            putExtra("video_url", video.url)
            putExtra("video_title", video.title)
            putExtra("is_streaming", true)
        }
        startActivity(intent)
    }

    private fun startDownload(video: DetectedVideo) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("video_url", video.url)
            putExtra("video_title", video.title)
            putExtra("auto_download", true)
        }
        startActivity(intent)
    }

    private fun handleDownload(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    ) {
        if (detectedVideos.none { it.url == url }) {
            detectedVideos.add(DetectedVideo(
                url = url,
                title = "Download",
                source = "Browser"
            ))
            updateDetectionBadge()
            showVideoDetectedToast()
        }
        
        AlertDialog.Builder(this)
            .setTitle(R.string.download_video)
            .setMessage(R.string.download_video_message)
            .setPositiveButton(R.string.download) { _, _ ->
                startDownload(DetectedVideo(url = url))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun shareCurrentPage() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, activeWebView?.url)
            putExtra(Intent.EXTRA_SUBJECT, activeWebView?.title)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun closeTab(tabId: Long) {
        lifecycleScope.launch {
            browserManager.closeTab(tabId)
            webViewContainer[tabId]?.let { webView ->
                binding.webViewContainer.removeView(webView)
                webView.destroy()
            }
            webViewContainer.remove(tabId)
            
            browserManager.getActiveTab()?.let { switchToTab(it) }
                ?: run { showHomePage() }
        }
    }

    private fun updateDetectionBadge() {
        val count = detectedVideos.size
        binding.tvDetectionBadge.text = count.toString()
        binding.tvDetectionBadge.isVisible = count > 0
        binding.btnDetectedVideos.isVisible = count > 0
        binding.bottomVideoActions.isVisible = count > 0
    }

    private fun showVideoDetectedToast() {
        Toast.makeText(this, R.string.video_detected, Toast.LENGTH_SHORT).show()
    }

    private fun isVideoUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".mp4") ||
               lower.contains(".webm") ||
               lower.contains(".m3u8") ||
               lower.contains(".mpd") ||
               lower.contains(".mkv") ||
               lower.contains("video") ||
               lower.contains("stream")
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.close_browser)
            .setMessage(R.string.close_browser_message)
            .setPositiveButton(R.string.yes) { _, _ -> finish() }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        webViewContainer.values.forEach { it.destroy() }
        webViewContainer.clear()
    }
    
    // Data classes
    data class QuickLink(val name: String, val url: String, val icon: Int)
}
