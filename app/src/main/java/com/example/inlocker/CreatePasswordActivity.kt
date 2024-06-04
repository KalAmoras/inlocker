package com.example.inlocker

import android.app.Activity
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

class CreatePasswordActivity : AppCompatActivity() {

    private lateinit var passwordDao: PasswordDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CreatePasswordActivity", "CreatePasswordActivity created")
        setContentView(R.layout.activity_create_password)

        val passwordEditText = findViewById<EditText>(R.id.passwordCreateText)
        val saveButton = findViewById<Button>(R.id.saveButton)

        val passwordDatabase = PasswordDatabase.getInstance(applicationContext)
        passwordDao = passwordDatabase.passwordDao()

        val chosenApp = intent.getStringExtra("chosenApp")
        val isSettingDefaultPassword = intent.getBooleanExtra("setDefaultPassword", false)

        val placeholderText = PlaceholderTextHelper.getPlaceholderTextOnCreatePassword(chosenApp)
        passwordEditText.hint = placeholderText

        saveButton.setOnClickListener {
            val password = passwordEditText.text.toString()
            Log.d("CreatePasswordActivity", "Save button clicked with password: $password")

            if (password.isNotBlank()) {
                if (isSettingDefaultPassword) {
                    setDefaultPasswords(password)
                } else {
                    savePassword(chosenApp!!, password)
                }
            } else {
                Log.d("CreatePasswordActivity", "Password is blank")
                Toast.makeText(this, "Password cannot be blank", Toast.LENGTH_SHORT).show()
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

    private fun setDefaultPasswords(password: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val functionalities = listOf("uninstall_protection", "delete_all_passwords")

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
}
