package com.kalsys.inlocker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kalsys.inlocker.ui.theme.InLockerTheme

class UsbDeviceActivity : ComponentActivity(), BatteryReceiver.BatteryLevelListener, BatteryReceiver.ChargingStatusListener {

    private lateinit var batteryReceiver: BatteryReceiver
    private var devices by mutableStateOf(listOf<UsbDeviceInfo>())
    private var isUsbCharging by mutableStateOf(false)
    private var isAcCharging by mutableStateOf(false)
    private var voltage by mutableDoubleStateOf(0.0)
    private var temperature by mutableDoubleStateOf(0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        batteryReceiver = BatteryReceiver().apply {
            setBatteryLevelListener(this@UsbDeviceActivity)
            setChargingStatusListener(this@UsbDeviceActivity)
            registerForBatteryChange(this@UsbDeviceActivity)
        }

        Log.d("UsbDeviceActivity", "BatteryReceiver registered")

        setContent {
            InLockerTheme {
                UsbDeviceScreen(devices, isUsbCharging, isAcCharging, voltage, temperature)
            }
        }
    }

    override fun onBatteryLevelChanged(level: Int) {
        // Handle battery level change if needed
    }

    override fun onChargingStatusChanged(isCharging: Boolean, usbCharge: Boolean, acCharge: Boolean) {
        isUsbCharging = usbCharge
        isAcCharging = acCharge
        Log.d("UsbDeviceActivity", "Battery state changed - isUsbCharging: $isUsbCharging, isAcCharging: $isAcCharging")
    }

    override fun onDestroy() {
        super.onDestroy()
        batteryReceiver.unregister(this)
        Log.d("UsbDeviceActivity", "BatteryReceiver unregistered")
    }
}

@Composable
fun UsbDeviceScreen(devices: List<UsbDeviceInfo>, isUsbCharging: Boolean, isAcCharging: Boolean, voltage: Double, temperature: Double) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Connected USB Devices", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        if (devices.isEmpty()) {
            Text("No USB devices connected")
        } else {
            devices.forEach { device ->
                UsbDeviceItem(device)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("USB Charging: ${if (isUsbCharging) "Yes" else "No"}")
        Text("AC Charging: ${if (isAcCharging) "Yes" else "No"}")
        Text("Voltage: $voltage V")
        Text("Temperature: $temperature °C")
    }
}

@Composable
fun UsbDeviceItem(device: UsbDeviceInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Name: ${device.name}")
            Text("Vendor: ${device.vendor}")
            Text("Vendor ID: ${device.vendorId}")
            Text("Product ID: ${device.productId}")
        }
    }
}

data class UsbDeviceInfo(val name: String, val vendor: String, val vendorId: String, val productId: String)

@Preview(showBackground = true)
@Composable
fun UsbDeviceScreenPreview() {
    InLockerTheme {
        UsbDeviceScreen(listOf(), false, false, 0.0, 0.0)
    }
}
