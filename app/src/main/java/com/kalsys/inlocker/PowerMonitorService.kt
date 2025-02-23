package com.kalsys.inlocker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class PowerMonitorService : Service() {
    private val batteryReceiver = BatteryReceiver()
    private var isMonitoring = false

    override fun onCreate() {
        super.onCreate()
        Log.d("PowerMonitorService", "Service created")
        startForegroundService()

        Log.d("PowerMonitorService", "Attempting to register BatteryReceiver")
        registerBatteryReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val shouldMonitor = intent?.getBooleanExtra("monitor", false) ?: false
        Log.d("PowerMonitorService", "onStartCommand called. Should monitor: $shouldMonitor, isMonitoring: $isMonitoring")

        if (shouldMonitor && !isMonitoring) {
            Log.d("PowerMonitorService", "Starting to monitor power connection changes.")
            isMonitoring = true
            registerBatteryReceiver()
        } else if (!shouldMonitor && isMonitoring) {
            Log.d("PowerMonitorService", "Stopping monitoring of power connection changes.")
            isMonitoring = false
            unregisterBatteryReceiver()
        } else if (!isMonitoring) {
            registerBatteryReceiver()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("PowerMonitorService", "Service is being destroyed")
        unregisterBatteryReceiver()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun registerBatteryReceiver() {
        if (!isMonitoring) {
            val intentFilter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
//                addAction(Intent.ACTION_BATTERY_CHANGED)
            }
            Log.d("PowerMonitorService", "Registering BatteryReceiver with actions: ${intentFilter.actionsIterator().asSequence().joinToString()}")
            registerReceiver(batteryReceiver, intentFilter)
            isMonitoring = true
        }
    }

    private fun unregisterBatteryReceiver() {
        try {
            unregisterReceiver(batteryReceiver)
            Log.d("PowerMonitorService", "BatteryReceiver unregistered successfully")
            isMonitoring = false
        } catch (e: IllegalArgumentException) {
            Log.d("PowerMonitorService", "Receiver was not registered or already unregistered")
        }
    }

    private fun startForegroundService() {
        Log.d("PowerMonitorService", "Starting service in foreground")

        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, "BatteryMonitorChannel")
            .setContentTitle("Battery Monitoring Service")
            .setContentText("Monitoring power connection changes.")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(2, notification)
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "BatteryMonitorChannel"
            val channelName = "Battery Monitoring"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "This channel is used for battery monitoring"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}
