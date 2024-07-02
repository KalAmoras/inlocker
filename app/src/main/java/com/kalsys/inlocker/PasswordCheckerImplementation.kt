package com.kalsys.inlocker

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class PasswordCheckerImplementation(
    private val passwordDao: PasswordDao,
    private val devicePolicyManager: DevicePolicyManager,
    private val compName: ComponentName
) : PasswordChecker {

    private val passwordTypes = listOf("uninstall_protection", "delete_all_passwords", "email_service")

    override suspend fun checkAndRequestPassword(
        context: Context,
        chosenApp: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
    )
    {
        if (chosenApp !in passwordTypes) {
            onFailure()
            return
        }

        val passwordItem = passwordDao.getPasswordItem(chosenApp)

        if (passwordItem == null) {
            context.startActivity(Intent(context, CreatePasswordActivity::class.java).apply {
                putExtra("chosenApp", chosenApp)
            })
        } else {
            Log.d("PasswordCheckerImplementation", "chosenApp: $chosenApp")

            when (chosenApp) {
                "uninstall_protection" -> {
                    Log.d("PasswordCheckerImplementation", "uninstall protection is being authenticated")

                    if (devicePolicyManager.isAdminActive(compName)) {
                        Log.d("PasswordCheckerImplementation", "Calling intent for uninstall protection disabling")

                        val intent = Intent(context, LockScreenActivity::class.java).apply {
                            putExtra("chosenApp", chosenApp)
                            Log.d("PasswordCheckerImplementation", "chosenApp inside uninstall_protection: $chosenApp")

                        }
                        Log.d("PasswordCheckerImplementation", "Sending result code of uninstall protection to AppOptionsActivity")

                        (context as AppCompatActivity).startActivityForResult(intent, CriticalSettingsActivity.REQUEST_CODE_LOCK_SCREEN)
                    } else {
                        onSuccess()
                    }
                }
                "delete_all_passwords" -> {
                    Log.d("PasswordCheckerImplementation", "Calling intent for deleting all passwords")

                    val intent = Intent(context, LockScreenActivity::class.java).apply {
                        putExtra("chosenApp", chosenApp)
                        Log.d("PasswordCheckerImplementation", "chosenApp inside delete all passwords: $chosenApp")
                    }
                    Log.d("PasswordCheckerImplementation", "Sending result code of delete all passwords to AppOptionsActivity")

                    (context as AppCompatActivity).startActivityForResult(intent, CriticalSettingsActivity.REQUEST_CODE_LOCK_SCREEN)
                }
                "email_service" -> {
                    Log.d("PasswordCheckerImplementation", "Calling intent for email service")

                    val intent = Intent(context, LockScreenActivity::class.java).apply {
                        putExtra("chosenApp", chosenApp)
                        Log.d("PasswordCheckerImplementation", "chosenApp inside email_service: $chosenApp")
                    }
                    Log.d("PasswordCheckerImplementation", "Sending result code of email service to CriticalSettingsActivity")

                    (context as AppCompatActivity).startActivityForResult(intent, CriticalSettingsActivity.REQUEST_CODE_LOCK_SCREEN)
                }
                else -> onFailure()
            }
        }
    }
}