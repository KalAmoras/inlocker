package com.kalsys.inlocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
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
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
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

        serviceScope.launch {
            val monitor = monitorDao.getMonitor()
            if (monitor?.shouldMonitor == true) {

            } else {
                stopForegroundService()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            Log.d("AppMonitorService", "onAccessibilityEvent triggered: ${event.eventType}, Window state changed, package: $packageName")
            if (!packageName.isNullOrEmpty() && !AuthStateManager.isAppAuthenticated(applicationContext, packageName)) {
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                if (!keyguardManager.isKeyguardLocked) {
                    GlobalScope.launch(Dispatchers.IO) {
                        val monitorItem = monitorDao.getMonitor()
                        if (monitorItem != null) {
                            Log.d(
                                "AppMonitorService",
                                "Monitor item found for package: ${monitorItem.shouldMonitor}"
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
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AppMonitorService", "Service started")

        serviceScope.launch {
            // Check the state of shouldMonitor in the monitor database
            val monitor = monitorDao.getMonitor()

            if (monitor?.shouldMonitor == true) {
                // Continue running the service if shouldMonitor is true
                Log.d("AppMonitorService", "Monitor is active, service will continue")
                // Ensure the service runs in the foreground
                startForeground(NOTIFICATION_ID, createNotification())
            } else {
                // Stop the service if shouldMonitor is false
                Log.d("AppMonitorService", "Monitor is inactive, stopping service")
                stopForegroundService()
            }
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }


    private fun stopForegroundService() {
        stopForeground(true)
        stopSelf()
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("AppMonitorService", "Attempting to create notification channel.")
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "App Monitor Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            Log.d("AppMonitorService", "Notification channel created: $CHANNEL_ID")

        }
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
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun promptDisableBatteryOptimizations() {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
//            restartService()
        }
    }

    override fun onInterrupt() {
    }
}