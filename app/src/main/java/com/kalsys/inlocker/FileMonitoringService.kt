package com.kalsys.inlocker

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.File

class FileMonitoringService : Service() {

    private val binder = LocalBinder()
    private var job: Job? = null
    private var lastModifiedMap = mutableMapOf<String, Long>()
    private lateinit var dataStoreManager: DataStoreManager

    override fun onCreate() {
        super.onCreate()
        dataStoreManager = DataStoreManager(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("FileMonitoringService", "Service started.")

        CoroutineScope(Dispatchers.IO).launch {
            val directoryUri = dataStoreManager.getFolderUri().first()
            if (directoryUri != null) {
                startMonitoring(Uri.parse(directoryUri))
            } else {
                Log.e("FileMonitoringService", "Directory URI is null, stopping service.")
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startMonitoring(directoryUri: Uri) {
        job = CoroutineScope(Dispatchers.IO).launch {
            Log.d("FileMonitoringService", "Started monitoring directory: $directoryUri")
            while (isActive) {
                checkForChanges(directoryUri)
                delay(60000)
            }
        }
    }

    private suspend fun checkForChanges(directoryUri: Uri) {
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        val cursor = contentResolver.query(
            directoryUri,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            val nameIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val modifiedIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex)
                val modified = it.getLong(modifiedIndex)

                val lastModified = lastModifiedMap[name] ?: 0
                if (modified > lastModified) {
                    lastModifiedMap[name] = modified
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

