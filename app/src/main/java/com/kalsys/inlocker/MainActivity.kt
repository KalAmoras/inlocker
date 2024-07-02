package com.kalsys.inlocker

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.kalsys.inlocker.ui.components.CustomButton
import com.kalsys.inlocker.ui.theme.InLockerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "com.kalsys.inlocker.prefs"
        private const val FIRST_LAUNCH_KEY = "first_launch"
        private const val MONITOR_SWITCH = "monitor_switch"
        private const val CREATE_PASSWORD_REQUEST_CODE = 1001

    }

    private val systemAlertWindowRequestCode = 101
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var monitorDao: MonitorDao
    private lateinit var passwordDao: PasswordDao
    private val switchState = mutableStateOf(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called")

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        monitorDao = PasswordDatabase.getInstance(applicationContext).monitorDao()
        passwordDao = PasswordDatabase.getInstance(applicationContext).passwordDao()


        if (isFirstLaunch()) {
            startActivity(Intent(this, InstructionActivity::class.java))
            markFirstLaunch()
            return
        }
        enableEdgeToEdge()
        setContent {
            InLockerTheme {
                if (!checkSystemAlertWindowPermission()) {
                    requestSystemAlertWindowPermission()
                }
                MyAppContent(
                    appName = getString(R.string.app_name)
                )
            }
        }
        checkAccessibilityServiceStatus()
        hasBackgroundStartPermissionInMIUI(this)
    }
    private fun enableService() {
        Log.d("MainActivity", "Running startForeground")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("MainActivity", "Before check for password on startForeground")

                val functionalities = listOf("uninstall_protection", "delete_all_passwords", "email_service")
                val nullPasswords = functionalities.any { functionality ->
                    val passwordItem = passwordDao.getPasswordItem(functionality)
                    Log.d("MainActivity", "Password item for $functionality: $passwordItem")
                    passwordItem?.password == null
                }

                Log.d("MainActivity", "Null passwords found: $nullPasswords")

                if (nullPasswords) {
                    withContext(Dispatchers.Main) {
                        Log.d("MainActivity", "Null passwords found, starting CreatePasswordActivity")

                        val intent = Intent(this@MainActivity, CreatePasswordActivity::class.java).apply {
                            putExtra("setDefaultPassword", true)
                        }
                        startActivityForResult(intent, CREATE_PASSWORD_REQUEST_CODE)
                    }
                } else {
                    startMonitoringService()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error in startForegroundService", e)
            }
        }
    }

    private fun disableService() {
        lifecycleScope.launch(Dispatchers.IO) {
            monitorDao.insertMonitor(Monitor(id = 1, shouldMonitor = false))
            sharedPreferences.edit().putBoolean(MONITOR_SWITCH, false).apply()

        }
        val intent = Intent(this, AppMonitorService::class.java)
        stopService(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_PASSWORD_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val functionalities = listOf("uninstall_protection", "delete_all_passwords", "email_service")
                    val nullPasswords = functionalities.any { functionality ->
                        val passwordItem = passwordDao.getPasswordItem(functionality)
                        Log.d("MainActivity", "Password item for $functionality: $passwordItem")
                        passwordItem?.password == null
                    }

                    if (!nullPasswords) {
                        withContext(Dispatchers.Main) {
                            startMonitoringService()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                "System wasn't enabled because it needs the passwords for critical functionalities.",
                                Toast.LENGTH_SHORT
                            ).show()
                            switchState.value = false

                        }
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    "System wasn't enabled because it needs the passwords for critical functionalities.",
                    Toast.LENGTH_SHORT
                ).show()
                switchState.value = false

            }
        }

    }

    private fun startMonitoringService() {
        Log.d("MainActivity", "No null passwords found, starting AppMonitorService")

        AuthStateManager.resetAuthState(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) {
            monitorDao.insertMonitor(Monitor(id = 1, shouldMonitor = true))
            sharedPreferences.edit().putBoolean(MONITOR_SWITCH, true).apply()

        }
        val intent = Intent(this, AppMonitorService::class.java)
        startService(intent)
    }

    private suspend fun getMonitorState(): Boolean {
        val state = withContext(Dispatchers.IO) {
            val monitorState = monitorDao.getMonitor()?.shouldMonitor ?: false
            sharedPreferences.edit().putBoolean(MONITOR_SWITCH, monitorState).apply()
            monitorState
        }
        return state
    }


    private fun requestSystemAlertWindowPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, systemAlertWindowRequestCode)
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
        }
        val activityInfo = intent.resolveActivityInfo(context.packageManager, 0)
        return activityInfo?.exported ?: false
    }

    private fun checkAccessibilityServiceStatus() {
        try {
            val isEnabled = isAccessibilityServiceEnabled(AppMonitorService::class.java)
//            Log.d("MainActivity", "Accessibility service status: $isEnabled")
            if (!isEnabled) {
                openAccessibilitySettings()
            }
//            Toast.makeText(this, "Accessibility is $isEnabled", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
//            Log.e("MainActivity", "Error checking Accessibility service status", e)
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
//            Log.d("MainActivity", "Checking service: $componentName")

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


    private fun isFirstLaunch(): Boolean {
        return !sharedPreferences.contains(FIRST_LAUNCH_KEY)
    }

    private fun markFirstLaunch() {
        sharedPreferences.edit().putBoolean(FIRST_LAUNCH_KEY, false).apply()
    }

     @Composable
    fun MyAppContent(
        appName: String
        )
    {
        val context = this@MainActivity
        val switchState = remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            switchState.value = getMonitorState()
        }
        Scaffold(modifier = Modifier
            .fillMaxSize()
        ) { innerPadding ->
            Row (
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ){
                Button(
                    onClick = {
                        val intent = Intent(context, InstructionActivity::class.java)
                        startActivity(intent)
                    },
                    modifier = Modifier.padding(end = 12.dp)
                        .padding(top = 40.dp)
                        .width(40.dp)
                        .height(40.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp) // Remove default padding
                ) {
                    Text(
                        text = "?",
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(top = 100.dp)
                    .padding(innerPadding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.inlocker_logo),
                    contentDescription = "Logo",
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .width(120.dp)
                        .height(120.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {

                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp),
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
                        onCheckedChange = { isChecked ->
                            switchState.value = isChecked
                            if (isChecked) {
                                enableService()
                            } else {
                                disableService()
                            }
                            sharedPreferences.edit().putBoolean(MONITOR_SWITCH, isChecked).apply()
                        },

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
                    CustomButton(
                        onClick = {
                            val intent = Intent(context, AppListActivity::class.java)
                            startActivity(intent)
                        },
                        modifier = Modifier.padding(8.dp),
                        text = "Set Apps Passwords"
                    )
                    CustomButton(
                        onClick = {
                            val intent = Intent(context, AppOptionsActivity::class.java)
                            startActivity(intent)
                        },
                        modifier = Modifier.padding(8.dp),
                        text = "Options"
                    )
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

}
