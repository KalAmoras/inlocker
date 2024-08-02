package com.kalsys.inlocker

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import android.Manifest
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.ui.text.style.TextAlign
import com.google.android.gms.common.AccountPicker
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.gmail.GmailScopes
import com.kalsys.inlocker.ui.components.CustomButton
import com.kalsys.inlocker.ui.theme.InLockerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

class EmailSettingsActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var emailService: EmailService
    private lateinit var credential: GoogleAccountCredential

    companion object {
        private const val REQUEST_ACCOUNT_PICKER = 1000
        private const val REQUEST_AUTHORIZATION = 1001
        private const val REQUEST_LOCATION_PERMISSION = 1002
        private const val EMAIL_PREF_KEY = "recipient_email"
        private const val SENDER_EMAIL = "inlockerkalsys@gmail.com"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestLocationPermission()

        sharedPreferences = getSharedPreferences("com.kalsys.inlocker", Context.MODE_PRIVATE)

        credential = GoogleAccountCredential.usingOAuth2(
            this, Collections.singletonList(GmailScopes.GMAIL_SEND)
        )

        val storedAccountName = getStoredRecipientEmail()
        if (storedAccountName != null) {
            credential.selectedAccountName = storedAccountName
        }

        emailService = EmailService(this, credential)


        setContent {
            InLockerTheme {
                EmailSettingsScreen(
                    onSetRecoveryEmail = { requestAccountPicker() },
                    onSendTestEmail = { sendTestEmail() },
                    onSendPasswordsEmail = { sendPasswordsEmail() }
                )
            }
        }
    }

    private fun requestAccountPicker() {
        val accountChooserOptions = AccountPicker.AccountChooserOptions.Builder()
            .setAllowableAccountsTypes(listOf("com.google"))
            .build()

        val intent = AccountPicker.newChooseAccountIntent(accountChooserOptions)
        startActivityForResult(intent, REQUEST_ACCOUNT_PICKER)
    }


    private fun getStoredRecipientEmail(): String? {
        val email = sharedPreferences.getString(EMAIL_PREF_KEY, null)
        Log.d("EmailSettingsActivity", "Getting stored recipient email: $email")
        return email
    }

    private fun setStoredRecipientEmail(email: String) {
        sharedPreferences.edit().putString(EMAIL_PREF_KEY, email).apply()
        Log.d("EmailSettingsActivity", "Setting stored recipient email to: $email")
    }
    private fun sendTestEmail() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("EmailSettingsActivity", "Sender email: $SENDER_EMAIL")
                val recipientEmail = getStoredRecipientEmail()
                Log.d("EmailSettingsActivity", "Recipient email before sending: $recipientEmail")
                if (recipientEmail.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EmailSettingsActivity, "Recipient email not set", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                emailService.sendEmail(
                    SENDER_EMAIL,
                    recipientEmail,
                    "Test Email from InLocker",
                    "This is a test email sent from InLocker."
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EmailSettingsActivity, "Test email sent successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: UserRecoverableAuthIOException) {
                withContext(Dispatchers.Main) {
                    startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
                }
            } catch (e: GoogleJsonResponseException) {
                Log.e("EmailSettingsActivity", "GoogleJsonResponseException: ${e.details.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EmailSettingsActivity, "Failed to send test email", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("EmailSettingsActivity", "Error sending test email: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EmailSettingsActivity, "Failed to send test email", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Location permission is required for this feature", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    private fun sendPasswordsEmail() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val recipientEmail = getStoredRecipientEmail()
                if (recipientEmail.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EmailSettingsActivity, "Recipient email not set", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                emailService.sendPasswordsEmail(SENDER_EMAIL, recipientEmail)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EmailSettingsActivity, "Passwords email sent successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: UserRecoverableAuthIOException) {
                withContext(Dispatchers.Main) {
                    startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
                }
            } catch (e: GoogleJsonResponseException) {
                Log.e("EmailSettingsActivity", "GoogleJsonResponseException: ${e.details.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EmailSettingsActivity, "Failed to send passwords email", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("EmailSettingsActivity", "Error sending passwords email: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EmailSettingsActivity, "Failed to send passwords email", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ACCOUNT_PICKER -> {
                if (resultCode == RESULT_OK && data != null) {
                    val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                    if (accountName != null) {
                        setStoredRecipientEmail(accountName)
                        credential.selectedAccountName = accountName
                        Toast.makeText(this, "Recipient email set to $accountName", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            REQUEST_AUTHORIZATION -> if (resultCode == RESULT_OK) {
                sendTestEmail()
            }
        }
    }

    @Composable
    fun EmailSettingsScreen(
        onSetRecoveryEmail: () -> Unit,
        onSendTestEmail: () -> Unit,
        onSendPasswordsEmail: () -> Unit,
    ) {
        val context = this@EmailSettingsActivity


        Row(
            modifier = Modifier.padding(bottom = 10.dp)
        ){
            Text(
                "Email Settings",
                fontSize = 34.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            )
        }
        Row( horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    val intent = Intent(context, EmailInstructionActivity::class.java)
                    startActivity(intent)
                },
                modifier = Modifier.padding(end = 12.dp)
                    .padding(top = 20.dp)
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
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,

        ) {

            Text(
                "This email will receive the list of passwords and the data sent by InLocker",
                fontSize = 16.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            )
            CustomButton(
                text = "Set Recovery Email",
                onClick = onSetRecoveryEmail,
                modifier = Modifier
                    .height(56.dp)
                    .width(142.dp),
                shape = RoundedCornerShape(6.dp)
            )
            Spacer(modifier = Modifier. height(10.dp))
            Text(
                "Send a test email to verify if your account is receiving emails from InLocker",
                fontSize = 16.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            )
            CustomButton(
                text = "Send Test Email",
                onClick = onSendTestEmail,
                modifier = Modifier
                    .height(56.dp)
                    .width(142.dp),
                shape = RoundedCornerShape(6.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Send the list of saved passwords to your recovery email",
                fontSize = 16.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            )
            CustomButton(
                text = "Send Passwords Email",
                onClick = onSendPasswordsEmail,
                modifier = Modifier
                    .height(56.dp)
                    .width(142.dp),
                shape = RoundedCornerShape(6.dp)
            )
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun EmailSettingsPreview() {
        InLockerTheme {
            EmailSettingsScreen(
                onSetRecoveryEmail = {},
                onSendTestEmail = {},
                onSendPasswordsEmail = {},
            )
        }
    }
}
