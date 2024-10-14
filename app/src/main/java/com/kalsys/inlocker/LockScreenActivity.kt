package com.kalsys.inlocker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Trace
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
import com.kalsys.inlocker.ui.screens.LockScreen
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
        Trace.beginSection("LockScreenActivity_onCreate")
        super.onCreate(savedInstanceState)

        Trace.beginSection("Initialize_Database")
        val passwordDatabase = PasswordDatabase.getInstance(applicationContext)
        passwordDao = passwordDatabase.passwordDao()
        Trace.endSection()

        Trace.beginSection("Initialize_Credentials")
        val credential = GoogleAccountCredential.usingOAuth2(
            this, Collections.singletonList(GmailScopes.GMAIL_SEND)
        )
        credential.selectedAccountName = getStoredRecipientEmail()
        emailService = EmailService(this, credential)
        Trace.endSection()

        Trace.beginSection("Initialize_Helpers")
        locationHelper = LocationHelper(this)
        cameraHelper = CameraHelper(this, this)
        Trace.endSection()


        appPackageName = intent.getStringExtra("chosenApp") ?: ""

        Trace.beginSection("SetContent")
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
        Trace.endSection()
        Trace.endSection()
    }

    private fun handlePasswordVerification(chosenApp: String) {
        val optionsList = listOf("critical_settings", "service_switch")

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

    private fun verifyPassword(password: String, onPasswordVerified: (String) -> Unit) {

        Trace.beginSection("verifyPassword")

        if (appPackageName.isNotEmpty()) {
            lifecycleScope.launch {

                Trace.beginSection("PasswordDao_getPasswordItem")
                val passwordItem = withContext(Dispatchers.IO) {
                    passwordDao.getPasswordItem(appPackageName)
                }
                Trace.endSection()


                try {
                    if (passwordItem != null && passwordItem.password == password) {
                        Trace.beginSection("SetAppAuthenticated")
                        AuthStateManager.setAppAuthenticated(applicationContext, appPackageName)
                        Trace.endSection()
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
        Trace.endSection()
    }

    private fun handleFailedPassword() {
        Trace.beginSection("handleFailedPassword")
        if (shouldSendEmail()) {
            Trace.beginSection("CameraHelper_takePhoto")
            cameraHelper.takePhoto { photoFile ->
                Trace.endSection()

                Trace.beginSection("LocationHelper_getCurrentLocation")
                locationHelper.getCurrentLocation { location ->
                    Trace.endSection()
                    location?.let {
                        val locationText = "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
                        val senderEmail = getStoredRecipientEmail()
                        val recipientEmail = getStoredRecipientEmail()
                        Log.d("LockScreenActivity", "Obtained location: Latitude = ${location.latitude}, Longitude = ${location.longitude}")

                        if (senderEmail != null && recipientEmail != null && photoFile != null) {
                            Trace.beginSection("EmailService_sendEmail")
                            emailService.sendLocationAndPhotoEmail(senderEmail, recipientEmail, locationText, photoFile)
                            saveLastEmailSentTime()
                            Trace.endSection()

                        } else {
                            Log.e("LockScreenActivity", "Email or photo is null, cannot send email")
                        }
                    } ?: run {
                        Log.e("LockScreenActivity", "Location is null, cannot send email")
                    }
                }
            }
        }
        Trace.endSection()

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

            launchIntent?.let {
                startActivity(it)
                finish()
            } ?: Toast.makeText(this, "Unable to launch app", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to launch app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getStoredRecipientEmail(): String? {
        val sharedPreferences = getSharedPreferences("com.kalsys.inlocker", Context.MODE_PRIVATE)
        return sharedPreferences.getString("recipient_email", null)
    }


    private fun getCurrentTimeMillis(): Long {
        val currentTimeMillis = System.currentTimeMillis()
        return currentTimeMillis
    }

    private fun shouldSendEmail(): Boolean {
        val sharedPreferences = getSharedPreferences("com.kalsys.inlocker", Context.MODE_PRIVATE)
        val lastSentTime = sharedPreferences.getLong("last_email_sent_time", 0)
        val currentTime = getCurrentTimeMillis()
        val shouldSend = (currentTime - lastSentTime) > emailIntervalMillis
        return shouldSend
    }

    private fun saveLastEmailSentTime() {
        val currentTime = getCurrentTimeMillis()
        val sharedPreferences = getSharedPreferences("com.kalsys.inlocker", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong("last_email_sent_time", currentTime).apply()
    }




    override fun onDestroy() {
        super.onDestroy()
        emailScope.cancel()
    }
}

