package com.pocketvscode

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
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
    private val PORT = 8080
    private val MAX_RETRIES = 60

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        loadingText = findViewById(R.id.loadingText)

        setupWebView()
        launchTermuxCodeServer()

        Thread {
            waitForServer()
        }.start()
    }

    private fun launchTermuxCodeServer() {
        // Tell Termux to run code-server via Termux:API intent
        val intent = Intent("com.termux.RUN_COMMAND")
        intent.setClassName("com.termux", "com.termux.app.RunCommandService")
        intent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/node")
        intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf(
            "/data/data/com.termux/files/home/code-server/out/node/entry.js",
            "--bind-addr", "0.0.0.0:8080",
            "--auth", "none",
            "--disable-telemetry"
        ))
        intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
        intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
        try {
            startService(intent)
            runOnUiThread { loadingText.text = "Starting VSCode via Termux..." }
        } catch (e: Exception) {
            runOnUiThread { loadingText.text = "Waiting for VSCode..." }
        }
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = false
        }
        webView.webViewClient = WebViewClient()
    }

    private fun waitForServer() {
        var retries = 0
        while (retries < MAX_RETRIES) {
            try {
                val conn = URL("http://127.0.0.1:$PORT").openConnection() as HttpURLConnection
                conn.connectTimeout = 1000
                conn.connect()
                if (conn.responseCode == 200) {
                    runOnUiThread { loadVSCode() }
                    return
                }
            } catch (e: Exception) { }
            retries++
            runOnUiThread {
                loadingText.text = "Waiting for VSCode... ($retries/$MAX_RETRIES)"
            }
            Thread.sleep(2000)
        }
        runOnUiThread {
            loadingText.text = "Could not connect. Open Termux and run:\nnode ~/code-server/out/node/entry.js --bind-addr 0.0.0.0:8080 --auth none"
        }
    }

    private fun loadVSCode() {
        loadingText.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl("http://127.0.0.1:$PORT")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
