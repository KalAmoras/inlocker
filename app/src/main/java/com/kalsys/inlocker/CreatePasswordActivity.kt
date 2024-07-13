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
import com.kalsys.inlocker.ui.theme.InLockerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreatePasswordActivity : AppCompatActivity() {


    private lateinit var passwordDao: PasswordDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CreatePasswordActivity", "CreatePasswordActivity created")

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

    @Composable
    fun CreatePasswordScreen(
        chosenApp: String?,
        isSettingDefaultPassword: Boolean,
        isSettingDefaultPasswordForAll: Boolean,
        onSavePassword: (String) -> Unit
    ) {
        var password by remember { mutableStateOf("") }
        val placeholderText = PlaceholderTextHelper.getPlaceholderTextOnCreatePassword(chosenApp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            PasswordTextField(
                label = placeholderText,
                password = password,
                onPasswordChange = { password = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (password.isNotBlank()) {
                        onSavePassword(password)
                    } else {
                        Toast.makeText(this@CreatePasswordActivity, "Password cannot be blank", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Password")
            }
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
                        Log.d("CreatePasswordActivity", "Password inserted for app: $chosenApp")
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
                    Log.e("CreatePasswordActivity", "Error saving password", e)
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
                    val functionalities = listOf("uninstall_protection", "delete_all_passwords", "email_service")

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
                Log.e("CreatePasswordActivity", "Setting default password failed: ${e.message}")
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
                    Log.d("CreatePasswordActivity", "Installed apps: ${installedApps.map { it.packageName }}")

                    //miui.home for the device home page ='desktop', globalminusscreen for the device manager (square button),
                    //latin for the device keyboard, system and systemui are called sometimes when browsing the device, still not clear
                    //
                    val excludedPackages = listOf(
                        "com.miui.home",
                        "com.mi.android.globalminusscreen",
                        "com.miui.system",
                        "com.android.systemui",
                        "com.google.android.inputmethod.latin",
                        "com.android.incallui",
                        "com.google.android.permissioncontroller")
                    val filteredApps = installedApps.filter { it.packageName !in excludedPackages }
                    Log.d("CreatePasswordActivity", "Filtered apps (excluding $excludedPackages): ${filteredApps.map { it.packageName }}")

                    val passwordItems = filteredApps.map { appInfo ->
                        PasswordItem(appInfo.packageName, defaultPassword)
                    }

                    passwordDao.insertOrUpdateAll(passwordItems)
                    Log.d("CreatePasswordActivity", "Password items inserted/updated: ${passwordItems.map { it.chosenApp }}")

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CreatePasswordActivity, "Default password set for all apps", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                } catch (e: Exception) {
                    Log.e("CreatePasswordActivity", "Error setting default passwords for all apps", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CreatePasswordActivity, "Error: Unable to set default password", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun CreatePasswordPreview() {
        InLockerTheme {
            CreatePasswordScreen(
                chosenApp = {}.toString(),
                isSettingDefaultPassword = false,
                isSettingDefaultPasswordForAll = false,
                onSavePassword = {}
            )
        }
    }

}
