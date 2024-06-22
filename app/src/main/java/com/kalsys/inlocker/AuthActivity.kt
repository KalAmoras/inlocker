package com.kalsys.inlocker

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStreamReader

class AuthActivity : AppCompatActivity() {

    private lateinit var credential: GoogleAccountCredential
    private val transport = NetHttpTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        credential = getCredentials()

        startActivityForResult(
            credential.newChooseAccountIntent(),
            REQUEST_ACCOUNT_PICKER
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ACCOUNT_PICKER -> if (resultCode == Activity.RESULT_OK && data != null && data.extras != null) {
                val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                if (accountName != null) {
                    credential.selectedAccountName = accountName
                    sendEmail(accountName)
                }
            }
        }
    }

    private fun sendEmail(accountName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val gmail = Gmail.Builder(transport, jsonFactory, credential)
                    .setApplicationName("InLocker")
                    .build()
            } catch (e: UserRecoverableAuthIOException) {
                startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getCredentials(): GoogleAccountCredential {
        val inputStream = resources.openRawResource(R.raw.client_secret)
        val clientSecrets = GoogleClientSecrets.load(jsonFactory, InputStreamReader(inputStream))
        return GoogleAccountCredential.usingOAuth2(this, listOf(GmailScopes.GMAIL_SEND))
            .setBackOff(ExponentialBackOff())
    }

    companion object {
        private const val REQUEST_ACCOUNT_PICKER = 1000
        private const val REQUEST_AUTHORIZATION = 1001
    }
}
