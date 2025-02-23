package com.kalsys.inlocker
import com.kalsys.inlocker.ui.screens.MainActivityScreen

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.kalsys.inlocker.ui.theme.InLockerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope


class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "com.kalsys.inlocker.prefs"
        private const val FIRST_LAUNCH_KEY = "first_launch"
        const val MONITOR_SWITCH = "monitor_switch"
        private const val CREATE_PASSWORD_REQUEST_CODE = 1001
        const val DISABLE_SERVICE_REQUEST_CODE = 1002
    }

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var monitorDao: MonitorDao
    private lateinit var passwordDao: PasswordDao
    private lateinit var passwordChecker: PasswordCheckerImplementation
    private lateinit var permissionManager: PermissionManager
    private val switchState = mutableStateOf(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called")
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        monitorDao = PasswordDatabase.getInstance(applicationContext).monitorDao()
        passwordDao = PasswordDatabase.getInstance(applicationContext).passwordDao()
        passwordChecker = PasswordCheckerImplementation(passwordDao)
        permissionManager = PermissionManager(this)

        enableEdgeToEdge()
        setContent {
            val switchState = remember { mutableStateOf(false) }

            InLockerTheme {
                MainActivityScreen(
                    appName = getString(R.string.app_name),
                    switchState = switchState,
                    sharedPreferences = sharedPreferences,
                    onEnableService = { enableService() },
                    onDisableService = { disableService() }
                )
            }
        }
        handlePermissionsAndInstructions()
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.Main).launch {
            val monitorState = getMonitorState()
            switchState.value = monitorState
        }
        permissionManager.checkAndRequestPermissions()
    }

    private fun enableService() {
        Log.d("MainActivity", "Running startForeground")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val missingPasswords = listOf("service_switch", "critical_settings").filter {
                    passwordDao.getPasswordItem(it)?.password == null
                }
                if (missingPasswords.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        for (functionality in missingPasswords) {
                            val intent = Intent(this@MainActivity, CreatePasswordActivity::class.java).apply {
                                putExtra("chosenApp", functionality)
                            }
                            startActivityForResult(intent, CREATE_PASSWORD_REQUEST_CODE)
                        }
                    }
                } else {
                    monitorDao.insertMonitor(Monitor(id = 1, shouldMonitor = true))
                    sharedPreferences.edit().putBoolean(MONITOR_SWITCH, true).apply()
                    startMonitoringService()
//                    startPowerMonitoringService()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error in enableService", e)
                withContext(Dispatchers.Main) {
                    switchState.value = false
                }
            }
        }
    }


    private fun disableService() {
        lifecycleScope.launch(Dispatchers.IO) {
            val intent = Intent(this@MainActivity, LockScreenActivity::class.java).apply {
                putExtra("chosenApp", "service_switch")
            }
            startActivityForResult(intent, DISABLE_SERVICE_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            DISABLE_SERVICE_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            monitorDao.insertMonitor(Monitor(id = 1, shouldMonitor = false))
                            sharedPreferences.edit().putBoolean(MONITOR_SWITCH, false).apply()
                            withContext(Dispatchers.Main) {
                                val appMonitorIntent = Intent(this@MainActivity, AppMonitorService::class.java)
                                stopService(appMonitorIntent)
//                                stopPowerMonitoringService()
                                showToast("Service stopped successfully")
                                switchState.value = false
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error disabling service", e)
                            switchState.value = true
                        }
                    }
                } else {
                    showToast("Failed to disable service due to incorrect password.")
                    switchState.value = true
                }
            }
            CREATE_PASSWORD_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val functionalities = listOf("service_switch", "critical_settings")
                            val nullPasswords = functionalities.any { functionality ->
                                val passwordItem = passwordDao.getPasswordItem(functionality)
                                passwordItem?.password == null
                            }
                            if (!nullPasswords) {
                                withContext(Dispatchers.Main) {
                                    AuthStateManager.resetAuthState(applicationContext)
                                    monitorDao.insertMonitor(Monitor(id = 1, shouldMonitor = true))
                                    sharedPreferences.edit().putBoolean(MONITOR_SWITCH, true).apply()
                                    startMonitoringService()
//                                    startPowerMonitoringService()
                                    switchState.value = true
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    showToast("System wasn't enabled because it needs passwords for critical functionalities.")
                                    switchState.value = false
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error handling CREATE_PASSWORD_REQUEST_CODE", e)
                            switchState.value = false
                        }
                    }
                } else {
                    showToast("System wasn't enabled because it needs passwords for critical functionalities.")
                    switchState.value = false
                }
            }
        }
    }


    private fun startMonitoringService() {
        AuthStateManager.resetAuthState(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) {
            val shouldMonitor = monitorDao.getMonitor()?.shouldMonitor ?: false
            sharedPreferences.edit().putBoolean(MONITOR_SWITCH, shouldMonitor).apply()
            val intent = Intent(this@MainActivity, AppMonitorService::class.java)
            startForegroundService(intent)
        }
    }

    private suspend fun getMonitorState(): Boolean {
        val state = withContext(Dispatchers.IO) {
            val monitorState = monitorDao.getMonitor()?.shouldMonitor ?: false
            sharedPreferences.edit().putBoolean(MONITOR_SWITCH, monitorState).apply()
            monitorState
        }
        return state
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun startPowerMonitoringService() {
        Log.d("MainActivity", "Starting PowerMonitoringService")
        val intent = Intent(this, PowerMonitorService::class.java)
        intent.putExtra("monitor", true)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopPowerMonitoringService() {
        Log.d("MainActivity", "Stopping PowerMonitoringService")
        val intent = Intent(this, PowerMonitorService::class.java)
        intent.putExtra("monitor", false)
        stopService(intent)
    }

    private fun handlePermissionsAndInstructions() {
        if (isFirstLaunch()) {
            startActivity(Intent(this, InstructionActivity::class.java))
            markFirstLaunch()
        } else {
            permissionManager.checkAndRequestPermissions()
        }
    }

    private fun isFirstLaunch(): Boolean {
        return !sharedPreferences.contains(FIRST_LAUNCH_KEY)
    }

    private fun markFirstLaunch() {
        sharedPreferences.edit().putBoolean(FIRST_LAUNCH_KEY, false).apply()
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { entry ->
            if (!entry.value) {
                Log.e("MainActivity", "Permission ${entry.key} not granted.")
            }
        }
    }
}
