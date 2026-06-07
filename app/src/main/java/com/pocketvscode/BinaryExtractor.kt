package com.pocketvscode

import android.content.Context
import android.util.Log
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.FileOutputStream

object BinaryExtractor {

    private const val TAG = "BinaryExtractor"
    private const val EXTRACTED_FLAG = "extracted_v2"

    fun extract(context: Context): Boolean {
        val filesDir = context.filesDir
        val flagFile = File(filesDir, EXTRACTED_FLAG)

        if (flagFile.exists()) {
            Log.d(TAG, "Already extracted")
            return true
        }

        return try {
            Log.d(TAG, "Extracting node binary...")
            extractAssetToFile(context, "node", File(filesDir, "node"))
            File(filesDir, "node").setExecutable(true, false)

            Log.d(TAG, "Extracting code-server.tar.gz...")
            extractTarGz(context, "code-server.tar.gz", filesDir)

            setExecutableRecursive(File(filesDir, "code-server/bin"))
            File(filesDir, "code-server/bin/code-server").setExecutable(true, false)

            flagFile.createNewFile()
            Log.d(TAG, "Extraction complete")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Extraction failed: ${e.message}", e)
            false
        }
    }

    private fun extractAssetToFile(context: Context, assetName: String, outFile: File) {
        context.assets.open(assetName).use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun extractTarGz(context: Context, assetName: String, destDir: File) {
        context.assets.open(assetName).use { assetStream ->
            GzipCompressorInputStream(assetStream).use { gzipStream ->
                TarArchiveInputStream(gzipStream).use { tarStream ->
                    var entry = tarStream.nextEntry
                    while (entry != null) {
                        val outFile = File(destDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { out ->
                                tarStream.copyTo(out)
                            }
                            if (entry.mode and 0b001001001 != 0) {
                                outFile.setExecutable(true, false)
                            }
                        }
                        entry = tarStream.nextEntry
                    }
                }
            }
        }
    }

    private fun setExecutableRecursive(dir: File) {
        dir.listFiles()?.forEach {
            it.setExecutable(true, false)
        }
    }
}
