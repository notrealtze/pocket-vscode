package com.pocketvscode

import android.content.Context
import android.os.Environment
import android.webkit.JavascriptInterface
import java.io.File

class StorageBridge(private val context: Context) {

    @JavascriptInterface
    fun getStoragePath(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    @JavascriptInterface
    fun listFiles(path: String): String {
        return try {
            val dir = File(path)
            if (!dir.exists() || !dir.isDirectory) return "[]"
            val files = dir.listFiles() ?: return "[]"
            "[" + files.joinToString(",") { f ->
                """{"name":"${f.name}","path":"${f.absolutePath}","isDir":${f.isDirectory},"size":${f.length()}}"""
            } + "]"
        } catch (e: Exception) { "[]" }
    }

    @JavascriptInterface
    fun readFile(path: String): String {
        return try { File(path).readText() } catch (e: Exception) { "" }
    }

    @JavascriptInterface
    fun writeFile(path: String, content: String): Boolean {
        return try { File(path).writeText(content); true } catch (e: Exception) { false }
    }
}
