package com.kalsys.inlocker

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Properties
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class EmailService(context: Context, credential: GoogleAccountCredential) {

    companion object {
        private const val TAG = "EmailService"
    }

    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val transport = NetHttpTransport()
    private val service: Gmail = Gmail.Builder(transport, jsonFactory, credential)
        .setApplicationName("InLocker")
        .build()
    private val passwordDao: PasswordDao = PasswordDatabase.getInstance(context).passwordDao()


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

    suspend fun sendLocationEmail(senderEmail: String, recipientEmail: String, location: String) {
        try {
            val subject = "Incorrect Password Attempt"
            val bodyText = "An incorrect password attempt was detected at the following location: $location"
            sendEmail(senderEmail, recipientEmail, subject, bodyText)
        } catch (e: UserRecoverableAuthIOException) {
            withContext(Dispatchers.Main) {
                throw e
            }
        } catch (e: GoogleJsonResponseException) {
            Log.e("EmailService", "GoogleJsonResponseException: ${e.details.message}", e)
        } catch (e: Exception) {
            Log.e("EmailService", "Error sending location email: ${e.message}", e)
        }
    }

    fun sendLocationAndPhotoEmail(sender: String, recipient: String, location: String, photoFile: File) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val mimeMessage = createEmailWithAttachment(sender, recipient, "Intrusion Alert", location, photoFile)
                sendMessage(mimeMessage)
            } catch (e: Exception) {
                Log.e("EmailService", "Error sending email: ${e.message}")
            }
        }
    }

    private fun createEmailWithAttachment(
        sender: String,
        recipient: String,
        subject: String,
        bodyText: String,
        file: File
    ): MimeMessage {
        val props = Properties()
        val session = Session.getDefaultInstance(props, null)
        val email = MimeMessage(session)

        email.setFrom(InternetAddress(sender))
        email.addRecipient(javax.mail.Message.RecipientType.TO, InternetAddress(recipient))
        email.subject = subject

        val mimeBodyPart = MimeBodyPart()
        mimeBodyPart.setContent(bodyText, "text/plain")

        val attachmentBodyPart = MimeBodyPart()
        attachmentBodyPart.attachFile(file)

        val multipart = MimeMultipart()
        multipart.addBodyPart(mimeBodyPart)
        multipart.addBodyPart(attachmentBodyPart)

        email.setContent(multipart)

        return email
    }

    suspend fun sendPasswordsEmail(senderEmail: String, recipientEmail: String) {
        try {
            val emailContent = buildPasswordEmailContent()
            val subject = "Your Saved Passwords"
            sendEmail(senderEmail, recipientEmail, subject, emailContent)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending passwords email: ${e.message}", e)
            throw e
        }
    }
    suspend fun buildPasswordEmailContent(): String {
        val passwords = passwordDao.getAllPasswords()
        val emailContent = StringBuilder("Here are your saved passwords:\n\n")
        passwords.forEach { passwordItem ->
            emailContent.append("App: ${passwordItem.chosenApp}\nPassword: ${passwordItem.password}\n\n")
        }
        return emailContent.toString()
    }
}

