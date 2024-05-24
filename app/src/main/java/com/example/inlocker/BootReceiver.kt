package com.example.inlocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, InitializationService::class.java)
            context.startForegroundService(serviceIntent)

            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val interval = prefs.getInt("check_interval", 60)

            JobSchedulerUtil.scheduleServiceRestartJob(context, interval)


        }
    }
}
