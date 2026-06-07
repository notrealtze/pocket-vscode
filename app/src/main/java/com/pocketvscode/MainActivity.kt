package com.pocketvscode

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.webkit.*
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var loadingText: TextView
    private var zoomLevel = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)
        loadingText = findViewById(R.id.loadingText)

        requestStoragePermission()
        setupWebView()
        setupZoomButtons()

        loadingText.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl("http://localhost:8080")
    }

    private fun setupZoomButtons() {
        findViewById<Button>(R.id.btnZoomIn).setOnClickListener {
            zoomLevel = (zoomLevel + 10).coerceAtMost(200)
            applyZoom()
        }
        findViewById<Button>(R.id.btnZoomOut).setOnClickListener {
            zoomLevel = (zoomLevel - 10).coerceAtLeast(50)
            applyZoom()
        }
    }

    private fun applyZoom() {
        webView.evaluateJavascript(
            "document.body.style.zoom='${zoomLevel}%'", null
        )
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }
    }

    private fun setupWebView() {
        WebView.setWebContentsDebuggingEnabled(true)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowUniversalAccessFromFileURLs = true
            allowFileAccessFromFileURLs = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    view.loadUrl("about:blank")
                    loadingText.visibility = View.VISIBLE
                    loadingText.text = "VSCode not running.\nOpen Termux and run:\nnode ~/code-server/out/node/entry.js --bind-addr 0.0.0.0:8080 --auth none"
                }
            }
        }
        webView.addJavascriptInterface(StorageBridge(this), "AndroidStorage")
    }
}
