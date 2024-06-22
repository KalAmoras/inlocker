package com.kalsys.inlocker

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.kalsys.inlocker.ui.components.CustomButton
import com.kalsys.inlocker.ui.components.PasswordTextField
import com.kalsys.inlocker.ui.theme.InLockerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LockScreenActivity : AppCompatActivity() {

    private lateinit var passwordDao: PasswordDao
    private lateinit var appPackageName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LockScreenActivity", "LockScreenActivity created")

        val passwordDatabase = PasswordDatabase.getInstance(applicationContext)
        passwordDao = passwordDatabase.passwordDao()

        appPackageName = intent.getStringExtra("chosenApp") ?: ""

        setContent {
            InLockerTheme {
                LockScreen(
                    appPackageName = appPackageName,
                    onUnlockClicked = { password ->
                        verifyPassword(password) { chosenApp ->
                            handlePasswordVerification(chosenApp)
                        }
                    }
                )
            }
        }
    }

    private fun handlePasswordVerification(chosenApp: String) {
        if (chosenApp == "uninstall_protection" || chosenApp == "delete_all_passwords") {
            val resultIntent = Intent().apply {
                putExtra("chosenApp", chosenApp)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        } else {
            launchApp(chosenApp)
        }
    }

    private fun launchApp(appPackageName: String) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                `package` = appPackageName
            }
            val launchIntent = packageManager.queryIntentActivities(intent, 0).firstOrNull()?.activityInfo?.let { activityInfo ->
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = ComponentName(activityInfo.packageName, activityInfo.name)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            if (launchIntent != null) {
                startActivity(launchIntent)
                finish()
            } else {
                Toast.makeText(this, "Unable to launch app", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to launch app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verifyPassword(password: String, onPasswordVerified: (String) -> Unit) {
        if (appPackageName.isNotEmpty()) {
            lifecycleScope.launch {
                val passwordItem = withContext(Dispatchers.IO) {
                    passwordDao.getPasswordItem(appPackageName)
                }

                if (passwordItem != null && passwordItem.password == password) {
                    AuthStateManager.setAppAuthenticated(applicationContext, appPackageName)
                    onPasswordVerified(appPackageName)
                } else {
                    Toast.makeText(this@LockScreenActivity, "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "App package name is null or empty", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun LockScreen(
    appPackageName: String,
    onUnlockClicked: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    val placeholderText = PlaceholderTextHelper.getPlaceholderText(appPackageName)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PasswordTextField(
            label = placeholderText,
            password = password,
            onPasswordChange = { password = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        CustomButton(
            text = "Unlock",
            onClick = {
                onUnlockClicked(password)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LockScreenPreview() {
    InLockerTheme {
        LockScreen(
            appPackageName = "com.example.app",
            onUnlockClicked = { /* Handle password verification in preview */ }
        )
    }
}
