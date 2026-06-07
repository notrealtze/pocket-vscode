package com.pocketvscode

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var loadingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)
        loadingText = findViewById(R.id.loadingText)
        setupWebView()
        Thread { waitForServer() }.start()
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        webView.webViewClient = WebViewClient()
    }

    private fun waitForServer() {
        var attempt = 0
        while (attempt < 120) {
            attempt++
            runOnUiThread { loadingText.text = "Starting VSCode... ($attempt/120)" }
            try {
                val conn = URL("http://localhost:8080").openConnection() as HttpURLConnection
                conn.connectTimeout = 1000
                conn.readTimeout = 1000
                conn.connect()
                if (conn.responseCode in 200..399) {
                    runOnUiThread {
                        loadingText.visibility = android.view.View.GONE
                        webView.visibility = android.view.View.VISIBLE
                        webView.loadUrl("http://localhost:8080")
                    }
                    return
                }
            } catch (_: Exception) {}
            Thread.sleep(1000)
        }
        runOnUiThread { loadingText.text = "Failed to connect. Is code-server running?" }
    }
}
