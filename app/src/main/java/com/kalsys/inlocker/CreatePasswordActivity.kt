package com.kalsys.inlocker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.kalsys.inlocker.ui.components.PasswordTextField
import com.kalsys.inlocker.ui.screens.CreatePasswordScreen
import com.kalsys.inlocker.ui.theme.InLockerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreatePasswordActivity : AppCompatActivity() {


    private lateinit var passwordDao: PasswordDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val passwordDatabase = PasswordDatabase.getInstance(applicationContext)
        passwordDao = passwordDatabase.passwordDao()

        val chosenApp = intent.getStringExtra("chosenApp")
        val isSettingDefaultPassword = intent.getBooleanExtra("setDefaultPassword", false)
        val isSettingDefaultPasswordForAll = intent.getBooleanExtra("setDefaultPasswordForAll", false)

        setContent {
            CreatePasswordScreen(
                chosenApp = chosenApp,
                isSettingDefaultPassword = isSettingDefaultPassword,
                isSettingDefaultPasswordForAll = isSettingDefaultPasswordForAll,
                onSavePassword = { password ->
                    if (isSettingDefaultPassword) {
                        setOptionsDefaultPassword(password)
                    } else if (isSettingDefaultPasswordForAll) {
                        setAppsDefaultPassword(password)
                    } else {
                        chosenApp?.let { savePassword(it, password) }
                    }
                }
            )
        }
    }

    private fun savePassword(chosenApp: String, password: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val existingPasswordItem = passwordDao.getPasswordItem(chosenApp)
                    if (existingPasswordItem != null) {
                        val updatedPasswordItem = PasswordItem(chosenApp, password)
                        passwordDao.update(updatedPasswordItem)
                    } else {
                        val newPasswordItem = PasswordItem(chosenApp, password)
                        passwordDao.insert(newPasswordItem)
                    }
                    val resultIntent = Intent().apply {
                        putExtra("chosenApp", chosenApp)
                        putExtra("newPassword", password)
                    }
                    withContext(Dispatchers.Main) {
                        setResult(Activity.RESULT_OK, resultIntent)
                        Toast.makeText(this@CreatePasswordActivity, "Password saved successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CreatePasswordActivity, "Error: Unable to save password", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setOptionsDefaultPassword(password: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val functionalities = listOf("service_switch", "critical_settings")

                    functionalities.forEach { functionality ->
                        val existingPasswordItem = passwordDao.getPasswordItem(functionality)
                        if (existingPasswordItem != null) {
                            val updatedPasswordItem = PasswordItem(functionality, password)
                            passwordDao.update(updatedPasswordItem)
                        } else {
                            val newPasswordItem = PasswordItem(functionality, password)
                            passwordDao.insert(newPasswordItem)
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CreatePasswordActivity, "Default password set for all functionalities", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CreatePasswordActivity, "Error: Unable to set default password", Toast.LENGTH_SHORT).show()
                }
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun setAppsDefaultPassword(defaultPassword: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val installedApps = packageManager.getInstalledApplications(0)

                    //miui.home for the device home page ='desktop', globalminusscreen for the device manager (square button),
                    //latin for the device keyboard, system and systemui are called sometimes when browsing the device, still not clear
                    //incallui for phone calls UI
                    val excludedPackages = listOf(
                        "com.miui.home",
                        "com.mi.android.globalminusscreen",
                        "com.miui.system",
                        "com.android.systemui",
                        "com.google.android.inputmethod.latin",
                        "com.android.incallui",
                        "com.google.android.permissioncontroller",
                        "com.android")
                    val filteredApps = installedApps.filter { it.packageName !in excludedPackages }
                    Log.d("CreatePasswordActivity", "Filtered apps (excluding $excludedPackages): ${filteredApps.map { it.packageName }}")

                    val passwordItems = filteredApps.map { appInfo ->
                        PasswordItem(appInfo.packageName, defaultPassword)
                    }

                    passwordDao.insertOrUpdateAll(passwordItems)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CreatePasswordActivity, "Default password set for all apps", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CreatePasswordActivity, "Error: Unable to set default password", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
