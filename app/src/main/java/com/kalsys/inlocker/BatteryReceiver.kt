package com.kalsys.inlocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import android.widget.Toast

class BatteryReceiver : BroadcastReceiver() {

    private var batteryLevelListener: BatteryLevelListener? = null
    private var chargingStatusListener: ChargingStatusListener? = null

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BatteryReceiver", "Received intent: ${intent.action}")
        when (intent.action) {
            Intent.ACTION_BATTERY_CHANGED -> handleBatteryChange(intent)
            Intent.ACTION_POWER_CONNECTED -> {
                Toast.makeText(context, "Power Connected", Toast.LENGTH_SHORT).show()
                Log.d("BatteryReceiver", "Power Connected")
                handlePowerConnection(context, intent)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                Toast.makeText(context, "Power Disconnected", Toast.LENGTH_SHORT).show()
                Log.d("BatteryReceiver", "Power Disconnected")
                handlePowerConnection(context, intent)
            }
        }
    }

    private fun handlePowerConnection(context: Context, intent: Intent) {
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB

        Log.d("BatteryReceiver", "USB Charging: $usbCharge, Charging: $isCharging")

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                if (!isServiceRunning(context, FileMonitoringService::class.java)) {
                    Log.d("BatteryReceiver", "Starting FileMonitoringService")
                    Toast.makeText(context, "Power connected. Monitoring started.", Toast.LENGTH_SHORT).show()
//                    context.startService(Intent(context, FileMonitoringService::class.java))
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                Log.d("BatteryReceiver", "Stopping FileMonitoringService")
                Toast.makeText(context, "Power disconnected. Monitoring stopped.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleBatteryChange(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC

        batteryLevelListener?.onBatteryLevelChanged(level)
        chargingStatusListener?.onChargingStatusChanged(isCharging, usbCharge, acCharge)
    }

    fun setBatteryLevelListener(listener: BatteryLevelListener) {
        this.batteryLevelListener = listener
    }

    fun setChargingStatusListener(listener: ChargingStatusListener) {
        this.chargingStatusListener = listener
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        Log.d("BatteryReceiver", "Checking if ${serviceClass.simpleName} is running")
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                Log.d("BatteryReceiver", "${serviceClass.simpleName} is running")
                return true
            }
        }
        Log.d("BatteryReceiver", "${serviceClass.simpleName} is not running")
        return false
    }

    fun registerForBatteryChange(context: Context) {
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        context.registerReceiver(this, intentFilter)
        Log.d("BatteryReceiver", "Receiver registered for battery changes")
    }

    fun unregister(context: Context) {
        try {
            context.unregisterReceiver(this)
            Log.d("BatteryReceiver", "Receiver unregistered")
        } catch (e: IllegalArgumentException) {
            Log.d("BatteryReceiver", "Receiver was not registered")
        }
    }

    interface BatteryLevelListener {
        fun onBatteryLevelChanged(level: Int)
    }

    interface ChargingStatusListener {
        fun onChargingStatusChanged(isCharging: Boolean, usbCharge: Boolean, acCharge: Boolean)
    }
}
