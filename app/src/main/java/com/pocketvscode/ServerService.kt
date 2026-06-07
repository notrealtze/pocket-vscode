package com.pocketvscode

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.io.File

class ServerService : Service() {

    private val TAG = "ServerService"
    private val CHANNEL_ID = "vscode_channel"
    private val NOTIF_ID = 1
    private var process: Process? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        startCodeServer()
    }

    private fun startCodeServer() {
        Thread {
            try {
                val filesDir = filesDir.absolutePath
                val node = "$filesDir/node"
                val codeServerEntry = "$filesDir/code-server/out/node/entry.js"
                val userDataDir = "$filesDir/vscode-data"
                val extensionsDir = "$filesDir/vscode-extensions"

                File(userDataDir).mkdirs()
                File(extensionsDir).mkdirs()

                val pb = ProcessBuilder(
                    node,
                    codeServerEntry,
                    "--bind-addr", "127.0.0.1:8080",
                    "--auth", "none",
                    "--user-data-dir", userDataDir,
                    "--extensions-dir", extensionsDir,
                    "--disable-telemetry"
                )
                pb.environment()["HOME"] = filesDir
                pb.environment()["TMPDIR"] = cacheDir.absolutePath
                pb.redirectErrorStream(true)

                process = pb.start()
                Log.d(TAG, "code-server started")

                process!!.inputStream.bufferedReader().forEachLine {
                    Log.d(TAG, it)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start code-server", e)
            }
        }.start()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "VSCode Server",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("PocketVSCode")
            .setContentText("VSCode server running")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .build()
    }

    override fun onDestroy() {
        process?.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
