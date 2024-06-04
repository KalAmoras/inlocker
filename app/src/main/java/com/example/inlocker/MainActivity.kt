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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inlocker.ui.theme.InLockerTheme

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
                MyAppContent(appName = getString(R.string.app_name))
            }
        }
        startForegroundService()
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
    fun MyAppContent(appName: String) {
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
            MyAppContent(appName = "InLocker")
        }
    }
    private fun startForegroundService() {
        val intent = Intent(this, AppMonitorService::class.java)
        startService(intent)
        Log.d("MainActivity", "Started AppMonitorService")
    }
}
