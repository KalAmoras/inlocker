package com.example.inlocker

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File


class AppOptionsActivity : AppCompatActivity() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private lateinit var passwordDao: PasswordDao


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        val deletePasswordsButton = findViewById<Button>(R.id.deletePasswordsButton)
        val setIntervalButton = findViewById<Button>(R.id.setIntervalButton)
        val resetAuthStateButton = findViewById<Button>(R.id.resetAuthStateButton)
        val intervalEditText = findViewById<EditText>(R.id.intervalEditText)

        deletePasswordsButton.setOnClickListener {
            deletePasswords()
        }

        setIntervalButton.setOnClickListener {
            val intervalText = intervalEditText.text.toString()
            if (intervalText.isNotEmpty()) {
                val interval = intervalText.toIntOrNull()
                if (interval != null && interval > 0) {
                    setJobSchedulerInterval(interval)
                } else {
                    Toast.makeText(this@AppOptionsActivity, "Minimum 1", Toast.LENGTH_SHORT).show()
                }
            }
        }

        resetAuthStateButton.setOnClickListener {
            resetAuthenticationState()
        }
    }

    private fun deletePasswords() {
        val passwordFile = File(filesDir, "vault.txt")
        if (passwordFile.exists()) {
            passwordFile.delete()
            Toast.makeText(this@AppOptionsActivity, "All passwords were deleted", Toast.LENGTH_SHORT).show()        }
    }

    private fun setJobSchedulerInterval(interval: Int) {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putInt("check_interval", interval)
            apply()
        }

        JobSchedulerUtil.scheduleServiceRestartJob(this, interval)
    }

    private fun resetAuthenticationState() {
        AuthStateManager.resetAuthState(applicationContext)
    }
}
