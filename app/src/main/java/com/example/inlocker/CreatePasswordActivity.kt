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

        saveButton.setOnClickListener {
            val password = passwordEditText.text.toString()
            Log.d("CreatePasswordActivity", "Save button clicked with password: $password")

            if (password.isNotBlank()) {
                val chosenApp = intent.getStringExtra("chosenApp")
                Log.d("CreatePasswordActivity", "Chosen app: $chosenApp")

                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            val existingPasswordItem = passwordDao.getPasswordItem(chosenApp!!)
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
            } else {
                Log.d("CreatePasswordActivity", "Password is blank")
                Toast.makeText(this, "Password cannot be blank", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
