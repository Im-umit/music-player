package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.bridge.MusicBridge
import com.example.data.mediastore.MediaStoreScanner
import com.example.service.MusicPlaybackService
import com.example.util.AlbumArtManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var musicBridge: MusicBridge
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isServiceBound = false

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
        val granted = permissions.entries.any { (perm, isGranted) ->
            (perm == Manifest.permission.READ_MEDIA_AUDIO || perm == Manifest.permission.READ_EXTERNAL_STORAGE) && isGranted
        }
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
                val scanner = MediaStoreScanner(this@MainActivity)
                val songs = scanner.scanSafFolder(uri)
                runOnUiThread {
                    webView.evaluateJavascript(
                        "window.onSafFolderScanned && window.onSafFolderScanned(${songs.size}, '${uri.toString()}')",
                        null
                    )
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure Chromium SimpleCache directory and index file exist to prevent simple_version_upgrade errors
        try {
            val httpCache = File(cacheDir, "WebView/Default/HTTP Cache")
            val indexDir = File(httpCache, "index-dir")
            if (!indexDir.exists()) {
                indexDir.mkdirs()
            }
            val indexFile = File(indexDir, "the-real-index")
            if (!indexFile.exists()) {
                indexFile.createNewFile()
            }
        } catch (e: Exception) {
            // ignore
        }

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

        activityScope.launch(Dispatchers.IO) {
            com.example.util.SampleMusicInstaller.installSampleMusic(this@MainActivity)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(useSoftwareRendering: Boolean = false) {
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
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE

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
                    val uriStr = reqUri.toString()

                    // Intercept modern https://music.local/albumart/* and legacy content://media/external/audio/albumart/*
                    if (uriStr.startsWith("content://media/external/audio/albumart") ||
                        (reqUri.host == "music.local" && reqUri.path?.contains("albumart") == true) ||
                        uriStr.contains("/albumart/")
                    ) {
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

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    checkAndRequestInitialPermissions()
                    try {
                        val codeCache = File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
                        if (codeCache.exists()) {
                            val wasmDir = File(codeCache, "wasm")
                            if (!wasmDir.exists()) wasmDir.mkdirs()
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
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
                    val crashed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) detail?.didCrash() else null
                    Log.w("MainActivity", "WebView render process crash caught (didCrash=$crashed). Recovering with software rendering fallback...")
                    try {
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
        webView.loadUrl("file:///android_asset/web/index.html")
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

    fun requestAudioPermission() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun checkAndRequestInitialPermissions() {
        if (!hasAudioPermission()) {
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
        webView.destroy()
        super.onDestroy()
    }
}
