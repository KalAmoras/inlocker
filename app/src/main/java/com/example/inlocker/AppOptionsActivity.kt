package com.example.inlocker

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.inlocker.ui.theme.InLockerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.appcompat.app.AppCompatActivity

class AppOptionsActivity : AppCompatActivity() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private lateinit var passwordDao: PasswordDao
    private lateinit var passwordChecker: PasswordChecker

    companion object {
        const val RESULT_ENABLE = 1
        const val REQUEST_CODE_LOCK_SCREEN = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        passwordDao = PasswordDatabase.getInstance(applicationContext).passwordDao()
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, MyDeviceAdminReceiver::class.java)
        passwordChecker = PasswordCheckerImplementation(passwordDao, devicePolicyManager, compName)

        setContent {

            InLockerTheme {
                val appCount = remember { mutableStateOf(0) }

                AppOptionsScreen(
                    onDeletePasswords = { showDeleteConfirmationDialog() },
                    onSetInterval = { interval -> setJobSchedulerInterval(interval) },
                    onResetAuthState = { resetAuthenticationState() },
                    onToggleUninstallProtection = { toggleUninstallProtection() },
                    isAdminActive = devicePolicyManager.isAdminActive(compName),
                    onCountInstalledApps = {
                        lifecycleScope.launch {
                            val count = getInstalledAppsCount()
                            Log.d("AppOptionsActivity", "Installed apps count: $count")
                            appCount.value = count
                        }
                        appCount.value
                    },
                    onSetDefaultPasswords = { setDefaultPassword() }
                )
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete All Passwords")
            .setMessage("Are you sure you want to delete all passwords?")
            .setPositiveButton(android.R.string.yes) { _, _ ->
                lifecycleScope.launch {
                    passwordChecker.checkAndRequestPassword(
                        this@AppOptionsActivity,
                        "delete_all_passwords",
                        { deletePasswords() },
                        {
                            Toast.makeText(this@AppOptionsActivity, "Password verification failed", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
            .setNegativeButton(android.R.string.no) { dialog, _ -> dialog.cancel() }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deletePasswords() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    passwordDao.deleteAllPasswords()
                }
                Toast.makeText(this@AppOptionsActivity, "All passwords were deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@AppOptionsActivity, "Error: Unable to delete passwords", Toast.LENGTH_SHORT).show()
                Log.e("AppOptionsActivity", "Deletion failed: ${e.message}")
            }
        }
    }

    private fun setJobSchedulerInterval(interval: Int) {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putInt("check_interval", interval)
            apply()
        }
        JobSchedulerUtil.scheduleServiceRestartJob(this, interval)
    }

    private fun resetAuthenticationState() {
        AuthStateManager.resetAuthState(applicationContext)
    }

    private fun toggleUninstallProtection() {
        Log.d("AppOptionsActivity", "toggleUninstallProtection called")
        if (devicePolicyManager.isAdminActive(compName)) {
            Log.d("AppOptionsActivity", "Device admin is active, requesting password")
            lifecycleScope.launch {
                try {
                    passwordChecker.checkAndRequestPassword(
                        this@AppOptionsActivity,
                        "uninstall_protection",
                        {
                            Log.d("AppOptionsActivity", "Password verified, disabling device admin")
                            disableDeviceAdmin()
                            Log.d("AppOptionsActivity", "Device admin disabled")
                        },
                        {
                            Log.d("AppOptionsActivity", "Password verification failed")
                            Toast.makeText(this@AppOptionsActivity, "Password verification failed", Toast.LENGTH_SHORT).show()
                        }
                    )
                } catch (e: Exception) {
                    Log.e("AppOptionsActivity", "Error checking password: ${e.message}", e)
                    Toast.makeText(this@AppOptionsActivity, "Error checking password", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.d("AppOptionsActivity", "Device admin is not active, enabling device admin")
            enableDeviceAdmin()
        }
    }


    private fun enableDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "You need to enable this to prevent uninstallation.")
        }
        startActivityForResult(intent, RESULT_ENABLE)
    }

    private fun disableDeviceAdmin() {
        try {
            Log.d("AppOptionsActivity", "Attempting to remove device admin")
            devicePolicyManager.removeActiveAdmin(compName)
            Log.d("AppOptionsActivity", "Device admin removed successfully")
            Toast.makeText(this, "Uninstall protection disabled", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("AppOptionsActivity", "Error disabling uninstall protection: ${e.message}")
            Toast.makeText(this, "Error disabling uninstall protection", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun getInstalledAppsCount(): Int {
        return withContext(Dispatchers.IO) {
            val packageManager = packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val installedApps = packageManager.queryIntentActivities(intent, 0)
                .map { it.activityInfo.applicationInfo }
            Log.d("AppOptionsActivity", "Installed apps retrieved: ${installedApps.size}")
            installedApps.size
        }
    }

    private fun setDefaultPassword() {
        val intent = Intent(this, CreatePasswordActivity::class.java).apply {
            putExtra("setDefaultPassword", true)
        }
        startActivity(intent)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("AppOptionsActivity", "onActivityResult called with requestCode: $requestCode, resultCode: $resultCode")
        when (requestCode) {
            RESULT_ENABLE -> {
                if (resultCode == RESULT_OK) {
                    Log.d("AppOptionsActivity", "RESULT_ENABLE returned RESULT_OK")
                    Toast.makeText(this, "Uninstall protection enabled", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to enable uninstall protection",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            REQUEST_CODE_LOCK_SCREEN -> {
                if (resultCode == RESULT_OK) {
                    Log.d("AppOptionsActivity", "REQUEST_CODE_LOCK_SCREEN returned RESULT_OK")

                    val passwordType = data?.getStringExtra("chosenApp")
                    if (passwordType != null) {
                        Log.d("AppOptionsActivity", "Password type: $passwordType")
                        when (passwordType) {
                            "uninstall_protection" -> {
                                Log.d("AppOptionsActivity", "Calling disableDeviceAdmin()")
                                disableDeviceAdmin()
                            }

                            "delete_all_passwords" -> {
                                Log.d("AppOptionsActivity", "Calling deletePasswords()")
                                deletePasswords()
                            }
                        }
                    } else {
                        Log.d("AppOptionsActivity", "Password type is null")
                    }
                } else {
                    Log.d(
                        "AppOptionsActivity",
                        "REQUEST_CODE_LOCK_SCREEN returned result code: $resultCode"
                    )
                    Toast.makeText(this, "Password verification failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppOptionsPreview() {
    InLockerTheme {
        AppOptionsScreen(
            onDeletePasswords = { },
            onSetInterval = { },
            onResetAuthState = {},
            onToggleUninstallProtection = {},
            isAdminActive = false,
            onCountInstalledApps = {42},
            onSetDefaultPasswords = {}
        )
    }
}

@Composable
fun AppOptionsScreen(
    onDeletePasswords: () -> Unit,
    onSetInterval: (Int) -> Unit,
    onResetAuthState: () -> Unit,
    onToggleUninstallProtection: () -> Unit,
    isAdminActive: Boolean,
    onCountInstalledApps: () -> Int,
    onSetDefaultPasswords: () -> Unit
) {

    var intervalText by remember { mutableStateOf("") }
    var installedAppCount by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = onDeletePasswords,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete All Passwords")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = intervalText,
            onValueChange = { intervalText = it },
            label = { Text("Set Interval") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { intervalText.toIntOrNull()?.let { onSetInterval(it) } },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set Interval")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onResetAuthState,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset Authentication State")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onToggleUninstallProtection,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isAdminActive) "Disable Uninstall Protection" else "Enable Uninstall Protection")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val count = onCountInstalledApps()
                Log.d("AppOptionsScreen", "Button clicked, installed apps count: $count")
                installedAppCount = count
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Count Installed Apps")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Installed Apps: $installedAppCount",
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onBackground
        )

        Button(
            onClick = onSetDefaultPasswords,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set Default Passwords")
        }
    }

}
