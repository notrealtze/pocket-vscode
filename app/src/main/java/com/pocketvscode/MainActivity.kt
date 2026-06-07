package com.pocketvscode

import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.Socket
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

    private fun isPortOpen(): Boolean {
        // Method 1: Socket (fastest, just checks if port is listening)
        try {
            Socket("localhost", 8080).use { return true }
        } catch (_: Exception) {}

        // Method 2: HttpURLConnection
        try {
            val conn = URL("http://localhost:8080").openConnection() as HttpURLConnection
            conn.connectTimeout = 1500
            conn.readTimeout = 1500
            conn.requestMethod = "GET"
            conn.connect()
            if (conn.responseCode in 200..399) return true
        } catch (_: Exception) {}

        // Method 3: HttpURLConnection with 127.0.0.1 explicitly
        try {
            val conn = URL("http://127.0.0.1:8080").openConnection() as HttpURLConnection
            conn.connectTimeout = 1500
            conn.readTimeout = 1500
            conn.instanceFollowRedirects = true
            conn.connect()
            if (conn.responseCode in 200..399) return true
        } catch (_: Exception) {}

        return false
    }

    private fun waitForServer() {
        var attempt = 0
        while (attempt < 120) {
            attempt++
            runOnUiThread { loadingText.text = "Starting VSCode... ($attempt/120)" }
            if (isPortOpen()) {
                runOnUiThread {
                    loadingText.visibility = View.GONE
                    webView.visibility = View.VISIBLE
                    webView.loadUrl("http://localhost:8080")
                }
                return
            }
            Thread.sleep(1000)
        }
        runOnUiThread {
            loadingText.text = "Timed out. Make sure code-server is running in Termux."
        }
    }
}
