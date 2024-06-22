package com.kalsys.inlocker

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalsys.inlocker.ui.theme.InLockerTheme

class MainActivity : ComponentActivity() {

    private val SYSTEM_ALERT_WINDOW_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called")

        enableEdgeToEdge()
        setContent {
            Log.d("MainActivity", "setContent called")
            InLockerTheme {

                if (!checkSystemAlertWindowPermission()) {
                    Log.d("MainActivity", "System alert window permission not granted")

                    requestSystemAlertWindowPermission()
                }
                MyAppContent(
                    appName = getString(R.string.app_name)
                )
            }
        }
        startForegroundService()
        checkAccessibilityServiceStatus()
        hasBackgroundStartPermissionInMIUI(this)
    }

    private fun requestSystemAlertWindowPermission() {
        Log.d("MainActivity", "Requesting system alert window permission")

        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, SYSTEM_ALERT_WINDOW_REQUEST_CODE)
    }

    private fun checkSystemAlertWindowPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    fun hasBackgroundStartPermissionInMIUI(context: Context): Boolean {
        val intent = Intent().apply {
            setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity")
            Log.d("MainActivity", "hasBackground: $intent")

        }
        val activityInfo = intent.resolveActivityInfo(context.packageManager, 0)
        return activityInfo?.exported ?: false
    }

    private fun checkAccessibilityServiceStatus() {
        try {
            val isEnabled = isAccessibilityServiceEnabled(AppMonitorService::class.java)
            Log.d("MainActivity", "Accessibility service status: $isEnabled")
            if (!isEnabled) {
                openAccessibilitySettings()
            }
            Toast.makeText(this, "Accessibility is $isEnabled", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking Accessibility service status", e)
        }
    }

    private fun isAccessibilityServiceEnabled(service: Class<out AccessibilityService>): Boolean {
        Log.d("MainActivity", "Checking if accessibility service is enabled")

        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

        if (enabledServices.isNullOrEmpty()) {
            return false
        }

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            Log.d("MainActivity", "Checking service: $componentName")

            if (componentName.equals(ComponentName(this, service).flattenToString(), ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e("MainActivity", "Error opening accessibility settings", e)
            Toast.makeText(this, "Unable to open Accessibility Settings", Toast.LENGTH_SHORT).show()
        }
    }
    @Composable
    fun MyAppContent(
        appName: String
        )
    {
        val switchState = remember { mutableStateOf(true) }
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp)
                ) {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (switchState.value) "Disable" else "Enable",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Switch(
                        checked = switchState.value,
                        onCheckedChange = { switchState.value = it },
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 180.dp)
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(this@MainActivity, AppListActivity::class.java)
                            startActivity(intent)
                        },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(text = "Set Apps Passwords")
                    }
                    Button(
                        onClick = {
                            val intent = Intent(this@MainActivity, AppOptionsActivity::class.java)
                            startActivity(intent)
                        },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(text = "Options")
                    }
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    private fun MyAppContentPreview() {
        InLockerTheme {
            MyAppContent(appName = "InLocker"
            )
        }
    }
    private fun startForegroundService() {
        Log.d("MainActivity", "Starting AppMonitorService")

        val intent = Intent(this, AppMonitorService::class.java)
        startService(intent)
        Log.d("MainActivity", "Started AppMonitorService")
    }
}
