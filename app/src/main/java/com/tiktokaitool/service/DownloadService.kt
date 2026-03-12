package com.tiktokaitool.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tiktokaitool.R
import com.tiktokaitool.utils.HistoryManager
import com.tiktokaitool.utils.TikTokUtils
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class DownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "tikai_download"
        var downloadCount = 0
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("url") ?: return START_NOT_STICKY

        val notifId = ++downloadCount + 2000
        startForeground(notifId, buildNotification("Preparing download...", 0))

        scope.launch {
            try {
                // Step 1: Resolve TikTok direct video URL
                val directUrl = TikTokUtils.resolveVideoUrl(url, client)
                    ?: throw Exception("Could not resolve video URL")

                // Step 2: Download
                val nm = getSystemService(NotificationManager::class.java)
                val req = Request.Builder().url(directUrl)
                    .addHeader("User-Agent", "TikTok 26.2.0 rv:262018 (iPhone; iOS 14.4.2; en_US) Cronet")
                    .build()

                val resp = client.newCall(req).execute()
                val body = resp.body ?: throw Exception("Empty response")
                val total = body.contentLength()

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val tikAiDir = File(downloadsDir, "TikAI").apply { mkdirs() }
                val outFile = File(tikAiDir, "tiktok_${System.currentTimeMillis()}.mp4")

                FileOutputStream(outFile).use { out ->
                    body.byteStream().use { input ->
                        val buf = ByteArray(8192)
                        var downloaded = 0L
                        var read: Int
                        while (input.read(buf).also { read = it } != -1) {
                            out.write(buf, 0, read)
                            downloaded += read
                            val progress = if (total > 0) ((downloaded * 100) / total).toInt() else -1
                            nm.notify(notifId, buildNotification("Downloading...", progress))
                        }
                    }
                }

                // Notify media scanner
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                    data = android.net.Uri.fromFile(outFile)
                })

                // Save to history
                HistoryManager(this@DownloadService).saveDownload(url, outFile.absolutePath)

                nm.notify(notifId, buildDoneNotification(outFile.name))

            } catch (e: Exception) {
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(notifId, buildErrorNotification(e.message ?: "Unknown error"))
            } finally {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun buildNotification(text: String, progress: Int): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⬇️ TikAI Download")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        if (progress >= 0) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    private fun buildDoneNotification(filename: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("✅ Download Complete")
            .setContentText("Saved: $filename")
            .setSmallIcon(R.drawable.ic_download)
            .setAutoCancel(true)
            .build()

    private fun buildErrorNotification(msg: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("❌ Download Failed")
            .setContentText(msg)
            .setSmallIcon(R.drawable.ic_download)
            .setAutoCancel(true)
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Downloads",
                NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
