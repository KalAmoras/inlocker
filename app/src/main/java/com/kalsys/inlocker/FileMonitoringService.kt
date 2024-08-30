package com.kalsys.inlocker

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.File

class FileMonitoringService : Service() {

    private val binder = LocalBinder()
    private lateinit var monitorDao: MonitorDao
    private var job: Job? = null
    private var lastModifiedMap = mutableMapOf<String, Long>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        CoroutineScope(Dispatchers.IO).launch {
            monitorDao = PasswordDatabase.getInstance(applicationContext).monitorDao()
            val monitor = monitorDao.getMonitor()

            withContext(Dispatchers.Main) {
                if (monitor?.shouldMonitor == true) {
                    startMonitoring()
                } else {
                    stopMonitoring()
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        val downloadUri = MediaStore.Files.getContentUri("external")
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                checkForChanges(downloadUri)
                delay(10000)
            }
        }
    }

    private suspend fun checkForChanges(directoryUri: Uri) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATA
        )

        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%Download%")

        val cursor = contentResolver.query(
            directoryUri,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val nameIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val modifiedIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val pathIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex)
                val modified = it.getLong(modifiedIndex)
                val path = it.getString(pathIndex)

                val lastModified = lastModifiedMap[path] ?: 0
                if (modified > lastModified) {
                    lastModifiedMap[path] = modified
                    notifyUser("File changed: $name")
                    Log.d("FileMonitoringService", "File changed: $name")
                }
            }
        }
    }

    private fun stopMonitoring() {
        job?.cancel()
        Log.d("FileMonitoringService", "Stopped monitoring")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d("FileMonitoringService", "Service bound")
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): FileMonitoringService = this@FileMonitoringService
    }

    private fun notifyUser(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this@FileMonitoringService, "channel_id")
            .setContentTitle("File Change Detected")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
