package com.kalsys.inlocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log

class BatteryReceiver : BroadcastReceiver() {

    var listener: BatteryListener? = null
    private var isRegistered = false
    private var isMonitoringBattery = false
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BatteryReceiver", "Received intent: ${intent.action}")

        val action = intent.action

        if (action == Intent.ACTION_POWER_CONNECTED || action == Intent.ACTION_POWER_DISCONNECTED) {
            handlePowerConnection(context, intent)
        } else if (action == Intent.ACTION_BATTERY_CHANGED) {
            handleBatteryChange(intent)
        }
    }

    private fun handlePowerConnection(context: Context, intent: Intent) {
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val action = intent.action

        Log.d("BatteryReceiver", "Usb: $usbCharge , Charging: $isCharging")

        if (action == Intent.ACTION_POWER_CONNECTED) {
            if (!isServiceRunning(context, FileMonitoringService::class.java)) {
                Log.d("BatteryReceiver", "Starting FileMonitoringService")
                context.startService(Intent(context, FileMonitoringService::class.java))
            }
        } else if (action == Intent.ACTION_POWER_DISCONNECTED) {
            Log.d("BatteryReceiver", "Stopping FileMonitoringService")
            context.stopService(Intent(context, FileMonitoringService::class.java))
        }
    }

    private fun handleBatteryChange(intent: Intent) {
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) / 1000.0 // Convert millivolts to volts
        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0 // Convert tenths of degree Celsius to degree Celsius
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC

        Log.d("BatteryReceiver", "Voltage: $voltage V, Temperature: $temperature °C, Usb: $usbCharge , Charging: $isCharging")

        listener?.onBatteryStateChanged(isCharging, usbCharge, acCharge, voltage, temperature)
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        Log.d("BatteryReceiver", "Checking if ${serviceClass.simpleName} is running")
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                Log.d("BatteryReceiver", "${serviceClass.simpleName} is running")
                return true
            }
        }
        Log.d("BatteryReceiver", "${serviceClass.simpleName} is not running")
        return false
    }
    fun registerForPowerConnection(context: Context) {
        if (!isRegistered) {
            val intentFilter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
            context.registerReceiver(this, intentFilter)
            isRegistered = true
            Log.d("BatteryReceiver", "Receiver registered for power connection")
        } else {
            Log.d("BatteryReceiver", "Receiver already registered for power connection")
        }
    }

    fun registerForBatteryChange(context: Context) {
        if (!isMonitoringBattery) {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(this, intentFilter)
            isMonitoringBattery = true
            Log.d("BatteryReceiver", "Receiver registered for battery changes")
        } else {
            Log.d("BatteryReceiver", "Receiver already registered for battery changes")
        }
    }
    fun unregister(context: Context) {
        if (isRegistered) {
            context.unregisterReceiver(this)
            isRegistered = false
            Log.d("BatteryReceiver", "Receiver unregistered")
        } else {
            Log.d("BatteryReceiver", "Receiver was not registered")
        }
    }


    interface BatteryListener {
        fun onBatteryStateChanged(isCharging: Boolean, usbCharge: Boolean, acCharge: Boolean, voltage: Double, temperature: Double)
    }
}
