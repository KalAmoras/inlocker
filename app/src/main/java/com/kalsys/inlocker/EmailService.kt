package com.kalsys.inlocker

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.Message
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Collections
import java.util.Properties
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class EmailService(context: Context, credential: GoogleAccountCredential) {

    companion object {
        private const val TAG = "EmailService"
    }

    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val transport = NetHttpTransport()
    private val service: Gmail = Gmail.Builder(transport, jsonFactory, credential)
        .setApplicationName("InLocker")
        .build()
    init {
        Log.d(TAG, "EmailService initialized with account: ${credential.selectedAccountName}")
    }
    fun sendEmail(senderEmail: String, recipientEmail: String, subject: String, bodyText: String) {
        try {
            val email = createEmail(senderEmail, recipientEmail, subject, bodyText)
            sendMessage(email)
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendEmail: ${e.message}", e)
            throw e
        }
    }

    private fun createEmail(from: String, to: String, subject: String, bodyText: String): MimeMessage {
        return try {
            val properties = Properties()
            val session = Session.getDefaultInstance(properties, null)
            val email = MimeMessage(session)

            email.setFrom(InternetAddress(from))
            email.addRecipient(javax.mail.Message.RecipientType.TO, InternetAddress(to))
            email.subject = subject
            email.setText(bodyText)
            Log.d(TAG, "Email created: To=$to, From=$from, Subject=$subject")
            email
        } catch (e: Exception) {
            Log.e(TAG, "Error in createEmail: ${e.message}", e)
            throw e
        }
    }

    private fun sendMessage(email: MimeMessage) {
        try {
            val buffer = ByteArrayOutputStream()
            email.writeTo(buffer)
            val rawMessageBytes = buffer.toByteArray()
            val encodedEmail = Base64.encodeToString(rawMessageBytes, Base64.URL_SAFE or Base64.NO_WRAP)
            val message = Message().setRaw(encodedEmail)
            Log.d(TAG, "Sending email message")
            service.users().messages().send("me", message).execute()
            Log.d(TAG, "Email sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}", e)
            throw e
        }
    }
}

