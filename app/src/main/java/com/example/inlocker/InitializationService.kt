package com.example.inlocker

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class InitializationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    companion object {
        private const val CHANNEL_ID = "InitializationServiceChannel"
        private const val CHANNEL_NAME = "Initialization Service"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("InitializationService", "Service created")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Initializing app settings..."))
        resetAuthStateAndStartAppMonitorService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("InitializationService", "Service started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d("InitializationService", "Service destroyed")
        serviceScope.cancel()
        restartService()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Initialization Service")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = createNotification(contentText)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun resetAuthStateAndStartAppMonitorService() {
        GlobalScope.launch(Dispatchers.IO) {
            AuthStateManager.resetAuthState(applicationContext)
            Log.d("InitializationService", "Auth state reset")

            updateNotification("Initialization complete. Locked.")

            val monitorServiceIntent = Intent(applicationContext, AppMonitorService::class.java)
            startService(monitorServiceIntent)
            stopSelf()
        }
    }

    private fun restartService() {
        val restartServiceIntent = Intent(applicationContext, InitializationService::class.java).also {
            it.setPackage(packageName)
        }
        val restartServicePendingIntent = PendingIntent.getService(
            this,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        )
    }
}
