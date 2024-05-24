// MainActivity.kt
package com.example.inlocker

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import com.example.inlocker.ui.theme.InLockerTheme
import java.io.File
import java.io.IOException


class MainActivity : ComponentActivity() {

    private val SYSTEM_ALERT_WINDOW_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InLockerTheme {
                if (!checkSystemAlertWindowPermission()) {
                    requestSystemAlertWindowPermission()
                }
                createVaultFileIfNotExists()
                MyAppContent()
            }
        }
        startForegroundService()
    }

    private fun createVaultFileIfNotExists() {
        val passwordFile = File(filesDir, "vault.txt")
        if (!passwordFile.exists()) {
            try {
                passwordFile.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun requestSystemAlertWindowPermission() {
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

    @Composable
    fun MyAppContent() {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                              Button(
                    onClick = {
                        val intent = Intent(this@MainActivity, AppListActivity::class.java)
                        startActivity(intent)
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(text = "Set Protected Apps")
                }

                Button(
                    onClick = {
                        val intent = Intent(this@MainActivity, AppOptionsActivity::class.java)
                        startActivity(intent)
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(text = "Set Options")
                }
            }
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, AppMonitorService::class.java)
        startService(intent)
        Log.d("MainActivity", "Started AppMonitorService")

    }
}
