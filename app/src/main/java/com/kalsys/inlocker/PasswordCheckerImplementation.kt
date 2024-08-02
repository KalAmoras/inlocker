package com.kalsys.inlocker

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class PasswordCheckerImplementation(
    private val passwordDao: PasswordDao,
) : PasswordChecker {

    private val passwordTypes = listOf("critical_settings", "service_switch")

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
                "service_switch" -> {
                    Log.d("PasswordCheckerImplementation", "Calling intent for service switch")

                    val intent = Intent(context, LockScreenActivity::class.java).apply {
                        putExtra("chosenApp", chosenApp)
                        Log.d("PasswordCheckerImplementation", "chosenApp inside service_switch: $chosenApp")
                    }
                    Log.d("PasswordCheckerImplementation", "Sending result code of service switch to MainActivity")

                    (context as AppCompatActivity).startActivityForResult(intent, MainActivity.DISABLE_SERVICE_REQUEST_CODE)
                }
                "critical_settings" -> {
                    Log.d("PasswordCheckerImplementation", "Calling intent for critical settings")

                    val intent = Intent(context, LockScreenActivity::class.java).apply {
                        putExtra("chosenApp", chosenApp)
                        Log.d("PasswordCheckerImplementation", "chosenApp inside critical_settings: $chosenApp")
                    }
                    Log.d("PasswordCheckerImplementation", "Sending result code of critical settings to AppOptionsActivity")

                    (context as AppCompatActivity).startActivityForResult(intent, AppOptionsActivity.REQUEST_CODE_LOCK_SCREEN)
                }
                else -> onFailure()
            }
        }
    }
}