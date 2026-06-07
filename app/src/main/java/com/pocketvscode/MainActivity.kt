package com.pocketvscode

import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import java.net.URL
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var loadingText: TextView
    private var zoomLevel = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)
        loadingText = findViewById(R.id.loadingText)
        setupWebView()
        setupZoomButtons()

        val uiDir = File(filesDir, "vscode-ui")
        if (uiDir.exists() && File(uiDir, "index.html").exists()) {
            loadEditor(uiDir)
        } else {
            Thread { downloadAndExtractUI(uiDir) }.start()
        }
    }

    private fun downloadAndExtractUI(uiDir: File) {
        try {
            runOnUiThread { loadingText.text = "Downloading VSCode UI (first time only)..." }
            uiDir.mkdirs()

            // Download code-server's static web UI only (much smaller than full code-server)
            val zipUrl = "https://github.com/coder/code-server/releases/download/v4.95.3/code-server-4.95.3-linux-amd64.tar.gz"
            // Actually grab just the static assets from the release
            val staticUrl = "https://cdn.jsdelivr.net/npm/code-server@4.95.3/dist/"

            // Use the standalone VSCode web from microsoft
            val vsixUrl = "https://update.code.visualstudio.com/latest/server-linux-arm64-web/stable"

            runOnUiThread { loadingText.text = "Downloading VSCode web UI..." }

            val zipFile = File(cacheDir, "vscode-web.tar.gz")
            URL(vsixUrl).openStream().use { input ->
                FileOutputStream(zipFile).use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        total += bytes
                        val mb = total / 1024 / 1024
                        runOnUiThread { loadingText.text = "Downloading... ${mb}MB" }
                    }
                }
            }

            runOnUiThread { loadingText.text = "Extracting..." }
            extractTarGz(zipFile, uiDir)
            zipFile.delete()

            runOnUiThread { loadEditor(uiDir) }
        } catch (e: Exception) {
            runOnUiThread {
                loadingText.text = "Download failed: ${e.message}\nCheck internet connection."
            }
        }
    }

    private fun extractTarGz(tarGz: File, destDir: File) {
        val process = Runtime.getRuntime().exec(arrayOf("tar", "-xzf", tarGz.absolutePath, "-C", destDir.absolutePath, "--strip-components=1"))
        process.waitFor()
    }

    private fun loadEditor(uiDir: File) {
        loadingText.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl("file://${uiDir.absolutePath}/index.html")
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
            allowContentAccess = true
            allowUniversalAccessFromFileURLs = true
            allowFileAccessFromFileURLs = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
            databaseEnabled = true
        }
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(StorageBridge(this), "AndroidStorage")
    }
}
