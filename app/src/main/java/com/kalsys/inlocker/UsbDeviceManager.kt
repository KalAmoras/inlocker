package com.kalsys.inlocker

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log

interface UsbDeviceListener {
    fun onDeviceAttached(device: UsbDevice)
    fun onDeviceDetached(device: UsbDevice)
}

class UsbDeviceManager(private val context: Context, private val listener: UsbDeviceListener) : BroadcastReceiver() {
    private val usbManager: UsbManager by lazy {
        context.getSystemService(Context.USB_SERVICE) as UsbManager
    }


    init {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        context.registerReceiver(this, filter)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                device?.let {
                    Log.d("UsbDeviceManager", "USB Device Attached: ${it.deviceName}")
                    listener.onDeviceAttached(it)
                }
            }
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                device?.let {
                    Log.d("UsbDeviceManager", "USB Device Detached: ${it.deviceName}")
                    listener.onDeviceDetached(it)
                }
            }
        }
    }

    fun listUsbDevices(): List<UsbDevice> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return usbManager.deviceList.values.toList()
    }

    fun requestUsbPermissionsIfNeeded(device: UsbDevice) {
        if (!usbManager.hasPermission(device)) {
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device, permissionIntent)
        }
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "com.kalsys.inlocker.USB_PERMISSION"
    }
}
