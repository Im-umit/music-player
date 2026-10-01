package com.neonmusic.player

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.database.ContentObserver
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.neonmusic.player.bridge.MusicBridge
import com.neonmusic.player.data.mediastore.MediaStoreScanner
import com.neonmusic.player.service.MusicPlaybackService
import com.neonmusic.player.util.AlbumArtManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var musicBridge: MusicBridge
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mediaRefreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var isServiceBound = false
    private var initialPermissionHandled = false

    private val mediaStoreObserver = object : ContentObserver(mediaRefreshHandler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            scheduleMediaStoreRefresh()
        }
    }

    private val mediaStoreRefreshRunnable = Runnable {
        if (isFinishing || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed)) return@Runnable
        if (::musicBridge.isInitialized && hasAudioPermission()) {
            musicBridge.scanLibrary()
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isServiceBound = true
            Log.d("MainActivity", "MusicPlaybackService connected successfully")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
            Log.d("MainActivity", "MusicPlaybackService disconnected")
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_AUDIO] == true || hasAudioPermission()
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true || hasAudioPermission()
        }
        val granted = audioPermission
        runOnUiThread {
            webView.evaluateJavascript("window.onPermissionResult && window.onPermissionResult($granted)", null)
            if (granted) {
                musicBridge.scanLibrary()
            }
        }
    }

    private val safFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            activityScope.launch(Dispatchers.IO) {
                try {
                    val scanner = MediaStoreScanner(this@MainActivity)
                    val songs = scanner.scanSafFolder(uri)
                    if (::musicBridge.isInitialized) musicBridge.invalidateLibraryCache()
                    runOnUiThread {
                        if (!::webView.isInitialized) return@runOnUiThread
                        val safeUri = org.json.JSONObject.quote(uri.toString())
                        webView.evaluateJavascript(
                            "window.onSafFolderScanned && window.onSafFolderScanned(${songs.size}, $safeUri)",
                            null
                        )
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "SAF folder scan failed", e)
                    val message = org.json.JSONObject.quote(e.message ?: "Klasör taraması başarısız oldu")
                    runOnUiThread {
                        if (::webView.isInitialized) {
                            webView.evaluateJavascript(
                                "window.onSafFolderScanFailed && window.onSafFolderScanFailed($message)",
                                null
                            )
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        initWebView()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::webView.isInitialized) {
                    webView.evaluateJavascript("window.handleAndroidBack ? window.handleAndroidBack() : false") { result ->
                        if (result != "true") {
                            if (webView.canGoBack()) {
                                webView.goBack()
                            } else {
                                isEnabled = false
                                onBackPressedDispatcher.onBackPressed()
                            }
                        }
                    }
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        ensureServiceStarted()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(useSoftwareRendering: Boolean = false) {
        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain("appassets.androidplatform.net")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Only fall back to software rendering after a real render-process crash.
            // Using LAYER_TYPE_NONE allows WebView to compose directly to hardware window without extra offscreen buffers.
            setLayerType(
                if (useSoftwareRendering) View.LAYER_TYPE_SOFTWARE else View.LAYER_TYPE_NONE,
                null
            )

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            // The app UI is served through WebViewAssetLoader, so direct file:// access
            // is unnecessary and disabled. This also removes file-origin attack surface.
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                settings.safeBrowsingEnabled = true
            }
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    Log.d(
                        "WebViewConsole",
                        "[${consoleMessage?.messageLevel()}] ${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})"
                    )
                    return true
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val reqUri = request?.url ?: return super.shouldInterceptRequest(view, request)

                    assetLoader.shouldInterceptRequest(reqUri)?.let { return it }

                    // Intercept modern https://music.local/albumart/* and legacy content://media/external/audio/albumart/*
                    val isLegacyAlbumArt = reqUri.scheme == "content" &&
                        reqUri.authority == android.provider.MediaStore.AUTHORITY &&
                        reqUri.path?.startsWith("/audio/albumart/") == true
                    val isLocalAlbumArt = reqUri.scheme == "https" &&
                        reqUri.host == "music.local" &&
                        reqUri.path?.startsWith("/albumart/") == true
                    if (isLegacyAlbumArt || isLocalAlbumArt) {
                        val artData = AlbumArtManager.getArtStream(this@MainActivity, reqUri)
                        val headers = mapOf(
                            "Access-Control-Allow-Origin" to "*",
                            "Cache-Control" to "max-age=86400"
                        )
                        return if (artData != null) {
                            WebResourceResponse(artData.second, null, 200, "OK", headers, artData.first)
                        } else {
                            // Return 404 cleanly so Chromium triggers onerror without calling AndroidProtocolHandler
                            WebResourceResponse(
                                "image/png",
                                null,
                                404,
                                "Not Found",
                                headers,
                                ByteArrayInputStream(ByteArray(0))
                            )
                        }
                    }

                    return super.shouldInterceptRequest(view, request)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val uri = request?.url ?: return false
                    val url = uri.toString()
                    val isLocalApp = uri.scheme == "https" &&
                        uri.host == "appassets.androidplatform.net" &&
                        uri.path?.startsWith("/assets/") == true
                    val isArtwork = uri.scheme == "https" && uri.host == "music.local" && uri.path?.startsWith("/albumart/") == true
                    if (isLocalApp || isArtwork) return false

                    // Never let the WebView navigate to arbitrary external pages.
                    // External links can still be handled by the user's browser when explicitly requested by app code.
                    return true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    checkAndRequestInitialPermissions()
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    Log.w("MainActivity", "WebView resource error: ${error?.description} (${request?.url})")
                }

                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?
                ): Boolean {
                    Log.w("MainActivity", "WebView render process crash caught (didCrash=${detail?.didCrash()}). Recovering with software rendering fallback...")
                    try {
                        if (::musicBridge.isInitialized) {
                            musicBridge.destroy()
                        }
                        view?.let {
                            (it.parent as? ViewGroup)?.removeView(it)
                            it.destroy()
                        }
                        initWebView(useSoftwareRendering = true)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error recreating WebView after render crash", e)
                    }
                    return true
                }
            }
        }

        musicBridge = MusicBridge(this, webView)
        webView.addJavascriptInterface(musicBridge, "AndroidMusicBridge")

        setContentView(webView)
        webView.loadUrl("https://appassets.androidplatform.net/assets/web/index.html")
    }

    fun hasAudioPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestAudioPermission() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasAudioPermission()) permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            if (!hasNotificationPermission()) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!hasAudioPermission()) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun checkAndRequestInitialPermissions() {
        if (initialPermissionHandled) return
        initialPermissionHandled = true
        if (!hasAudioPermission() || !hasNotificationPermission()) {
            requestAudioPermission()
        } else {
            musicBridge.scanLibrary()
        }
    }

    fun launchSafFolderPicker() {
        safFolderLauncher.launch(null)
    }

    fun ensureServiceStarted() {
        try {
            val serviceIntent = Intent(this, MusicPlaybackService::class.java)
            startService(serviceIntent)
            if (!isServiceBound) {
                bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "ensureServiceStarted error: ${e.message}", e)
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            val externalAudioUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            contentResolver.registerContentObserver(externalAudioUri, true, mediaStoreObserver)
        } catch (e: Exception) {
            Log.w("MainActivity", "MediaStore observer registration failed: ${e.message}")
        }
    }

    private fun scheduleMediaStoreRefresh() {
        mediaRefreshHandler.removeCallbacks(mediaStoreRefreshRunnable)
        mediaRefreshHandler.postDelayed(mediaStoreRefreshRunnable, 1500L)
    }

    override fun onStop() {
        mediaRefreshHandler.removeCallbacks(mediaStoreRefreshRunnable)
        try {
            contentResolver.unregisterContentObserver(mediaStoreObserver)
        } catch (e: Exception) {
            Log.w("MainActivity", "MediaStore observer unregister failed: ${e.message}")
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (hasAudioPermission()) {
            if (::webView.isInitialized) {
                // onAppResumed() already re-reads the (fast, local) database and refreshes
                // the UI on the JS side. Triggering a full native MediaStore rescan here too
                // (as before) meant every time the app came back to the foreground it did a
                // heavy filesystem scan *and* two full library re-renders back to back,
                // which is what caused the stutter/freeze when switching back into the app.
                // A full rescan is still available explicitly from the library menu.
                webView.evaluateJavascript("window.onAppResumed && window.onAppResumed()", null)
            }
        }
    }

    override fun onDestroy() {
        if (isServiceBound) {
            try {
                unbindService(serviceConnection)
                isServiceBound = false
            } catch (e: Exception) {
                Log.e("MainActivity", "unbindService error: ${e.message}", e)
            }
        }
        if (::musicBridge.isInitialized) {
            musicBridge.destroy()
        }
        mediaRefreshHandler.removeCallbacks(mediaStoreRefreshRunnable)
        activityScope.cancel()
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
