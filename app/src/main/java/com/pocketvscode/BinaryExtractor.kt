package com.pocketvscode

import android.content.Context
import android.util.Log
import java.io.File

object BinaryExtractor {

    private const val TAG = "BinaryExtractor"
    private const val EXTRACTED_FLAG = "extracted_v1"

    fun extract(context: Context): Boolean {
        val filesDir = context.filesDir
        val flagFile = File(filesDir, EXTRACTED_FLAG)

        if (flagFile.exists()) {
            Log.d(TAG, "Already extracted")
            return true
        }

        return try {
            extractAsset(context, "code-server.tar.gz", filesDir)
            val pb = ProcessBuilder("tar", "-xzf",
                File(filesDir, "code-server.tar.gz").absolutePath,
                "-C", filesDir.absolutePath)
            pb.redirectErrorStream(true)
            val proc = pb.start()
            proc.waitFor()

            val nodeFile = File(filesDir, "node")
            if (!nodeFile.exists()) {
                copyNodeBinary(context, filesDir)
            }

            setExecutable(filesDir)
            flagFile.createNewFile()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Extraction failed", e)
            false
        }
    }

    private fun extractAsset(context: Context, name: String, dest: File) {
        val outFile = File(dest, name)
        context.assets.open(name).use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun copyNodeBinary(context: Context, dest: File) {
        extractAsset(context, "node", dest)
    }

    private fun setExecutable(dir: File) {
        listOf("node", "code-server/bin/code-server").forEach {
            val f = File(dir, it)
            if (f.exists()) f.setExecutable(true, false)
        }
    }
}
