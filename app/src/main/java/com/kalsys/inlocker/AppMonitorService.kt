package com.kalsys.inlocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AppMonitorService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var passwordDao: PasswordDao
    private lateinit var monitorDao: MonitorDao

    companion object {
        private const val CHANNEL_ID = "AppMonitorServiceChannel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AppMonitorService", "Service connected")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations()) {
                promptDisableBatteryOptimizations()
            }
        }

        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        serviceInfo = info

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        val passwordDatabase = PasswordDatabase.getInstance(applicationContext)
        passwordDao = passwordDatabase.passwordDao()
        monitorDao = passwordDatabase.monitorDao()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            Log.d("AppMonitorService", "onAccessibilityEvent triggered: ${event.eventType}, Window state changed, package: $packageName")
            if (!packageName.isNullOrEmpty() && !AuthStateManager.isAppAuthenticated(applicationContext, packageName)) {
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                if (!keyguardManager.isKeyguardLocked) {
                    serviceScope.launch(Dispatchers.IO) {
                        val monitorItem = monitorDao.getMonitor()
                        if (monitorItem != null) {
                            Log.d(
                                "AppMonitorService","Monitor item found for package: ${monitorItem.shouldMonitor}"
                            )
                            if (monitorItem.shouldMonitor === true) {
                                val passwordItem = passwordDao.getPasswordItem(packageName)
                                if (passwordItem != null) {
                                    Log.d("AppMonitorService","Password item found for package: $packageName")
                                    val lockScreenIntent = Intent(applicationContext, LockScreenActivity::class.java).apply {
                                        putExtra("chosenApp", packageName)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        startActivity(lockScreenIntent)
                                    } catch (e: Exception) {
                                        Log.e("AppMonitorService","Error launching LockScreenActivity: ${e.message}")
                                    }
                                } else {
                                    Log.d(
                                        "AppMonitorService",
                                        "No password item found for package: $packageName"
                                    )
                                }
                            } else {
                                stopForegroundService()
                            }
                        }
                    }
                } else {
                    Log.d("AppMonitorService", "Device is locked")
                }
            } else {
                Log.d("AppMonitorService", "App is already authenticated or package name is null")
            }
        } else {
            Log.d("AppMonitorService", "Event type not handled: ${event.eventType}")
        }
        } catch (e: Exception) {
            Log.e("AppMonitorService", "Error in onAccessibilityEvent: ${e.message}", e)
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AppMonitorService", "Service started")
        serviceScope.launch {
            try {
                val monitor = monitorDao.getMonitor()
                if (monitor?.shouldMonitor == true) {
                    Log.d("AppMonitorService", "Monitor is active, service will continue")
                    startForeground(NOTIFICATION_ID, createNotification())
                } else {
                    Log.d("AppMonitorService", "Monitor is inactive, stopping service")
                }
            } catch (e: Exception) {
                Log.e("AppMonitorService", "Error in onStartCommand: ${e.message}", e)
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, AppMonitorService::class.java).apply {
            setPackage(packageName)
        }
        val restartServicePendingIntent = PendingIntent.getService(
            this, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_RUNNING_LOW, TRIM_MEMORY_RUNNING_CRITICAL -> {
                Log.w("AppMonitorService", "Memory is running low. Releasing resources.")
                clearCaches()
            }
            TRIM_MEMORY_BACKGROUND, TRIM_MEMORY_UI_HIDDEN -> {
                Log.d("AppMonitorService", "App is in the background. Consider releasing UI-related resources.")
            }
            TRIM_MEMORY_COMPLETE -> {
                Log.w("AppMonitorService", "System is under extreme memory pressure. Freeing up everything possible.")
                releaseCoroutines()
            }

            TRIM_MEMORY_MODERATE -> {
                Log.d("AppMonitorService", "Memory is moderately low. Releasing non-critical resources.")
            }
            TRIM_MEMORY_RUNNING_MODERATE -> {
                Log.d("AppMonitorService", "Memory pressure moderate. Monitoring resource usage.")
            }
        }
    }

    private fun clearCaches() {

        val cacheDir = applicationContext.cacheDir
        if (cacheDir.isDirectory) {
            cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        }

        Log.d("AppMonitorService", "Caches cleared.")
    }

    private fun releaseCoroutines() {
        serviceScope.cancel()
        Log.d("AppMonitorService", "Releasing all resources.")
    }

    private fun stopForegroundService() {
        stopForeground(true)
        stopSelf()
    }


    private fun createNotificationChannel() {
            Log.d("AppMonitorService", "Attempting to create notification channel.")
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "App Monitor Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            Log.d("AppMonitorService", "Notification channel created: $CHANNEL_ID")
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("InLocked")
            .setContentText("You won't get rid of me so easily...")
            .setSmallIcon(R.drawable.inlocker_eye_icon)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
    }

    private fun promptDisableBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.e("AppMonitorService", "Battery optimization settings not found: ${e.message}")
            }
        }
    }

    override fun onInterrupt() {
    }
}