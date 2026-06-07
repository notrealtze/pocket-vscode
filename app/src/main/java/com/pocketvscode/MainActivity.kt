package com.pocketvscode

import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var loadingText: TextView
    private var zoomLevel = 100
    private val VSCODE_URL = "https://foolnah4i4i4-vscode.hf.space"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)
        loadingText = findViewById(R.id.loadingText)
        setupWebView()
        setupZoomButtons()
        loadingText.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl(VSCODE_URL)
    }

    private fun setupZoomButtons() {
        findViewById<Button>(R.id.btnZoomIn).setOnClickListener {
            zoomLevel = (zoomLevel + 10).coerceAtMost(200)
            webView.evaluateJavascript("document.body.style.zoom='${zoomLevel}%'", null)
        }
        findViewById<Button>(R.id.btnZoomOut).setOnClickListener {
            zoomLevel = (zoomLevel - 10).coerceAtLeast(50)
            webView.evaluateJavascript("document.body.style.zoom='${zoomLevel}%'", null)
        }
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
            databaseEnabled = true
            userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    loadingText.visibility = View.VISIBLE
                    loadingText.text = "Connection lost. Check internet."
                }
            }
        }
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(StorageBridge(this), "AndroidStorage")
    }
}
