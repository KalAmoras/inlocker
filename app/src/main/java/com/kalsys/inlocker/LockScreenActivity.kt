package com.kalsys.inlocker

import android.content.ComponentName
import android.content.Context
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
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.gmail.GmailScopes
import com.kalsys.inlocker.ui.components.CustomButton
import com.kalsys.inlocker.ui.components.PasswordTextField
import com.kalsys.inlocker.ui.theme.InLockerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

class LockScreenActivity : AppCompatActivity() {

    private lateinit var passwordDao: PasswordDao
    private lateinit var appPackageName: String
    private lateinit var emailService: EmailService
    private lateinit var cameraHelper: CameraHelper
    private lateinit var locationHelper: LocationHelper
    private val emailIntervalMillis: Long = 60000
    private val emailScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LockScreenActivity", "LockScreenActivity created")

        val passwordDatabase = PasswordDatabase.getInstance(applicationContext)
        passwordDao = passwordDatabase.passwordDao()

        val credential = GoogleAccountCredential.usingOAuth2(
            this, Collections.singletonList(GmailScopes.GMAIL_SEND)
        )
        credential.selectedAccountName = getStoredRecipientEmail()
        emailService = EmailService(this, credential)
        locationHelper = LocationHelper(this)
        cameraHelper = CameraHelper(this, this)

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
        val optionsList = listOf("uninstall_protection", "delete_all_passwords", "email_service")

        if(optionsList.contains(chosenApp)){
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

                try {
                    if (passwordItem != null && passwordItem.password == password) {
                        AuthStateManager.setAppAuthenticated(applicationContext, appPackageName)
                        onPasswordVerified(appPackageName)
                    } else {
                        handleFailedPassword()
                        Log.d("LockScreenActivity", "Else block reached on verifyPassword")
                        Toast.makeText(this@LockScreenActivity, "Incorrect password", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("LockScreenActivity", "Exception in verifyPassword: ${e.message}")
                }
            }
        } else {
            Toast.makeText(this, "App package name is null or empty", Toast.LENGTH_SHORT).show()
        }
    }



    private fun getStoredRecipientEmail(): String? {
        val sharedPreferences = getSharedPreferences("com.kalsys.inlocker", Context.MODE_PRIVATE)
        return sharedPreferences.getString("recipient_email", null)
    }


    private fun getCurrentTimeMillis(): Long {
        val currentTimeMillis = System.currentTimeMillis()
        Log.d("LockScreenActivity", "Current time in millis: $currentTimeMillis")
        return currentTimeMillis
    }

    private fun shouldSendEmail(): Boolean {
        val sharedPreferences = getSharedPreferences("com.kalsys.inlocker", Context.MODE_PRIVATE)
        val lastSentTime = sharedPreferences.getLong("last_email_sent_time", 0)
        val currentTime = getCurrentTimeMillis()
        val shouldSend = (currentTime - lastSentTime) > emailIntervalMillis
        Log.d("LockScreenActivity", "Last sent time: $lastSentTime, Current time: $currentTime, Should send email: $shouldSend")
        return shouldSend
    }

    private fun saveLastEmailSentTime() {
        val currentTime = getCurrentTimeMillis()
        val sharedPreferences = getSharedPreferences("com.kalsys.inlocker", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong("last_email_sent_time", currentTime).apply()
        Log.d("LockScreenActivity", "Saved last email sent time: $currentTime")
    }

//    private fun handleFailedPassword() {
//        if (shouldSendEmail()) {
//            locationHelper.getCurrentLocation { location ->
//                location?.let {
//                    val locationText = "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
//                    val senderEmail = getStoredRecipientEmail()
//                    val recipientEmail = getStoredRecipientEmail()
//                    Log.d("LockScreenActivity", "Obtained location: Latitude = ${location.latitude}, Longitude = ${location.longitude}")
//                    senderEmail?.let { sender ->
//                        emailScope.launch {
//                            emailService.sendLocationEmail(sender, recipientEmail!!, locationText)
//                            saveLastEmailSentTime()
//                        }
//                    }
//                }
//            }
//        } else {
//            Log.d("LockScreenActivity", "Email not sent due to interval restriction")
//        }
//    }
    private fun handleFailedPassword() {
        if (shouldSendEmail()) {
            cameraHelper.takePhoto { photoFile ->
                locationHelper.getCurrentLocation { location ->
                    location?.let {
                        val locationText = "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
                        val senderEmail = getStoredRecipientEmail()
                        val recipientEmail = getStoredRecipientEmail()
                        Log.d("LockScreenActivity", "Obtained location: Latitude = ${location.latitude}, Longitude = ${location.longitude}")

                        if (senderEmail != null && recipientEmail != null && photoFile != null) {
                            emailService.sendLocationAndPhotoEmail(senderEmail, recipientEmail, locationText, photoFile)
                            saveLastEmailSentTime()
                        } else {
                            Log.e("LockScreenActivity", "Email or photo is null, cannot send email")
                        }
                    } ?: run {
                        Log.e("LockScreenActivity", "Location is null, cannot send email")
                    }
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        emailScope.cancel()
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
            onUnlockClicked = {}
        )
    }
}
