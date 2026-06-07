package com.pocketvscode

import android.content.Context
import android.util.Log
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object BinaryExtractor {

    private const val TAG = "BinaryExtractor"
    private const val EXTRACTED_FLAG = "extracted_v3"
    private const val NODE_URL = "https://nodejs.org/dist/v20.19.0/node-v20.19.0-linux-arm64.tar.gz"
    private const val CS_URL = "https://github.com/coder/code-server/releases/download/v4.95.3/code-server-4.95.3-linux-arm64.tar.gz"

    fun extract(context: Context, onProgress: (String) -> Unit): Boolean {
        val filesDir = context.filesDir
        val flagFile = File(filesDir, EXTRACTED_FLAG)

        if (flagFile.exists()) {
            Log.d(TAG, "Already extracted")
            return true
        }

        return try {
            onProgress("Downloading Node.js...")
            Log.d(TAG, "Downloading node...")
            val nodeTar = File(filesDir, "node.tar.gz")
            downloadFile(NODE_URL, nodeTar)

            onProgress("Extracting Node.js...")
            Log.d(TAG, "Extracting node binary...")
            extractNodeBinary(nodeTar, filesDir)
            nodeTar.delete()

            onProgress("Downloading code-server (150MB)...")
            Log.d(TAG, "Downloading code-server...")
            val csTar = File(filesDir, "cs.tar.gz")
            downloadFile(CS_URL, csTar)

            onProgress("Extracting code-server...")
            Log.d(TAG, "Extracting code-server...")
            extractTarGz(csTar, filesDir, "code-server")
            csTar.delete()

            setExecutableRecursive(filesDir)

            flagFile.createNewFile()
            Log.d(TAG, "Done")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed: ${e.message}", e)
            false
        }
    }

    private fun downloadFile(url: String, dest: File) {
        URL(url).openStream().use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun extractNodeBinary(tarGz: File, destDir: File) {
        TarArchiveInputStream(GzipCompressorInputStream(tarGz.inputStream())).use { tar ->
            var entry = tar.nextEntry
            while (entry != null) {
                if (entry.name.endsWith("/bin/node") && !entry.isDirectory) {
                    val out = File(destDir, "node")
                    FileOutputStream(out).use { tar.copyTo(it) }
                    out.setExecutable(true, false)
                    return
                }
                entry = tar.nextEntry
            }
        }
    }

    private fun extractTarGz(tarGz: File, destDir: File, topFolder: String) {
        TarArchiveInputStream(GzipCompressorInputStream(tarGz.inputStream())).use { tar ->
            var entry = tar.nextEntry
            while (entry != null) {
                val name = entry.name.replaceFirst(Regex("^[^/]+"), topFolder)
                val outFile = File(destDir, name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { tar.copyTo(it) }
                    if (entry.mode and 0b001001001 != 0) outFile.setExecutable(true, false)
                }
                entry = tar.nextEntry
            }
        }
    }

    private fun setExecutableRecursive(dir: File) {
        File(dir, "code-server/bin").listFiles()?.forEach { it.setExecutable(true, false) }
        File(dir, "node").setExecutable(true, false)
    }
}
