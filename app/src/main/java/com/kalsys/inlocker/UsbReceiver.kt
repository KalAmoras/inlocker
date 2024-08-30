package com.kalsys.inlocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.util.Log
import android.widget.Toast

class UsbReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("UsbReceiver", "onReceive called")
        when (intent?.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                context?.let {
                    Toast.makeText(it, "USB Device Connected", Toast.LENGTH_SHORT).show()
                    Log.d("UsbReceiver", "USB DEVICE CONNECTED")
                }
            }
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                context?.let {
                    Toast.makeText(it, "USB Device Disconnected", Toast.LENGTH_SHORT).show()
                    Log.d("UsbReceiver", "USB DEVICE DISCONNECTED")
                }
            }
            "com.kalsys.inlocker.USB_PERMISSION" -> {
            }
            else -> {
                Log.d("UsbReceiver", "Unknown Action: ${intent?.action}")
            }
        }
    }
}
