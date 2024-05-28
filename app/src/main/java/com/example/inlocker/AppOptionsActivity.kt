package com.example.inlocker

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class AppOptionsActivity : AppCompatActivity() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private lateinit var passwordDao: PasswordDao


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        passwordDao = PasswordDatabase.getInstance(applicationContext).passwordDao()

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, MyDeviceAdminReceiver::class.java)

        val deletePasswordsButton = findViewById<Button>(R.id.deletePasswordsButton)
        val setIntervalButton = findViewById<Button>(R.id.setIntervalButton)
        val resetAuthStateButton = findViewById<Button>(R.id.resetAuthStateButton)
        val intervalEditText = findViewById<EditText>(R.id.intervalEditText)
        val uninstallProtectionButton = findViewById<Button>(R.id.enableUninstallProtectionButton)


        deletePasswordsButton.setOnClickListener {
            deletePasswords()
        }

        setIntervalButton.setOnClickListener {
            val intervalText = intervalEditText.text.toString()
            if (intervalText.isNotEmpty()) {
                val interval = intervalText.toIntOrNull()
                if (interval != null && interval > 0) {
                    setJobSchedulerInterval(interval)
                } else {
                    Toast.makeText(this@AppOptionsActivity, "Minimum 1", Toast.LENGTH_SHORT).show()
                }
            }
        }

        resetAuthStateButton.setOnClickListener {
            resetAuthenticationState()
        }

        uninstallProtectionButton.setOnClickListener {
            checkAndRequestPassword()
        }

        updateUninstallProtectionButtonText()

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

    private fun checkAndRequestPassword() {
        CoroutineScope(Dispatchers.Main).launch {
            val passwordItem = passwordDao.getPasswordItem("uninstall_protection")
            val chosenApp = "uninstall_protection"

            if (passwordItem == null) {
                startActivity(Intent(this@AppOptionsActivity, CreatePasswordActivity::class.java).apply {
                    putExtra("chosenApp", chosenApp)
                })
            } else {
                if (devicePolicyManager.isAdminActive(compName)) {
                    val intent = Intent(this@AppOptionsActivity, LockScreenActivity::class.java).apply {
                        putExtra("chosenApp", chosenApp)
                    }
                    startActivityForResult(intent, REQUEST_CODE_LOCK_SCREEN)
                } else {
                    enableDeviceAdmin()
                }
            }
        }
    }


    private fun enableDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "You need to enable this to prevent uninstallation.")
        startActivityForResult(intent, RESULT_ENABLE)
        updateUninstallProtectionButtonText()

    }

    private fun disableDeviceAdmin() {
        devicePolicyManager.removeActiveAdmin(compName)
        Toast.makeText(this, "Uninstall protection disabled", Toast.LENGTH_SHORT).show()
        updateUninstallProtectionButtonText()

    }
    private fun updateUninstallProtectionButtonText() {
        val uninstallProtectionButton = findViewById<Button>(R.id.enableUninstallProtectionButton)
        if (devicePolicyManager.isAdminActive(compName)) {
            uninstallProtectionButton.text = "Disable Uninstall Protection"
        } else {
            uninstallProtectionButton.text = "Enable Uninstall Protection"
        }
    }
    companion object {
        const val RESULT_ENABLE = 1
        const val REQUEST_CODE_LOCK_SCREEN = 2
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_ENABLE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Uninstall protection enabled", Toast.LENGTH_SHORT).show()
                updateUninstallProtectionButtonText()
            } else {
                Toast.makeText(this, "Failed to enable uninstall protection", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == REQUEST_CODE_LOCK_SCREEN) {
            if (resultCode == RESULT_OK) {
                disableDeviceAdmin()
            } else {
                Toast.makeText(this, "Password verification failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
