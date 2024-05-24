package com.example.inlocker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LockScreenActivity : AppCompatActivity() {

    private lateinit var passwordDao: PasswordDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LockScreenActivity", "LockScreenActivity created")
        try {
            setContentView(R.layout.activity_lock_screen)

            val passwordEditText: EditText = findViewById(R.id.passwordEditText)
            val unlockButton: Button = findViewById(R.id.unlockButton)

            val passwordDatabase = PasswordDatabase.getInstance(applicationContext)
            passwordDao = passwordDatabase.passwordDao()

            unlockButton.setOnClickListener {
                val enteredPassword = passwordEditText.text.toString()
                Log.d("LockScreenActivity", "Entered password: $enteredPassword")

                val appPackageName = intent.getStringExtra("chosenApp")
                Log.d("LockScreenActivity", "Chosen app: $appPackageName")

                if (!appPackageName.isNullOrEmpty()) {
                    lifecycleScope.launch {
                        val passwordItem = withContext(Dispatchers.IO) {
                            Log.d("LockScreenActivity", "Fetching password item for $appPackageName")

                            passwordDao.getPasswordItem(appPackageName)
                        }

                        if (passwordItem != null && passwordItem.password == enteredPassword) {
                            AuthStateManager.setAppAuthenticated(applicationContext, appPackageName)
                            launchApp(appPackageName)
                        } else {
                            Log.d("LockScreenActivity", "Password mismatch or item not found")
                            Toast.makeText(this@LockScreenActivity, "Incorrect password", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.d("LockScreenActivity", "App package name is null or empty")
                }
            }
        } catch (e: Exception) {
            Log.e("LockScreenActivity", "Exception in onCreate: ${e.message}", e)
        }
    }

    private fun launchApp(appPackageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(appPackageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
            finish()
        } else {
            Toast.makeText(this, "Unable to launch app", Toast.LENGTH_SHORT).show()
        }
    }
}
