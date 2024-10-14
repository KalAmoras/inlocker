package com.kalsys.inlocker

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.kalsys.inlocker.ui.components.CustomButton
import com.kalsys.inlocker.ui.theme.InLockerTheme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.kalsys.inlocker.CriticalSettingsActivity.Companion.RESULT_ENABLE
import com.kalsys.inlocker.ui.screens.AppOptionsScreen
import kotlinx.coroutines.launch


class AppOptionsActivity : AppCompatActivity() {

    private lateinit var passwordChecker: PasswordCheckerImplementation
    private lateinit var passwordDao: PasswordDao

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        val REQUEST_CODE_LOCK_SCREEN = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        passwordDao = PasswordDatabase.getInstance(applicationContext).passwordDao()
        passwordChecker = PasswordCheckerImplementation(passwordDao)

        setContent {
            InLockerTheme {
                var isJobScheduled by remember { mutableStateOf(isJobScheduled()) }
                AppOptionsScreen(
                    onSetInterval = { interval, showToast -> setJobSchedulerInterval(interval, showToast) },
                    onResetAuthState = { resetAuthenticationState() },
                    onNavigateToCriticalOptions = { navigateToCriticalOptions() },
                    isJobScheduled = isJobScheduled,
                    onToggleJobScheduler = {
                        toggleJobScheduler()
                        isJobScheduled = isJobScheduled()
                    }
                )
            }
        }
    }

    private fun isJobScheduled(): Boolean {
        return prefs.getBoolean("is_job_scheduled", false)
    }

    private fun setJobScheduled(value: Boolean) {
        with(prefs.edit()) {
            putBoolean("is_job_scheduled", value)
            apply()
        }
    }

    private fun toggleJobScheduler() {
        if (isJobScheduled()) {
            stopJobScheduler()
        } else {
            startJobScheduler()
        }
        setJobScheduled(!isJobScheduled())
    }

    private fun startJobScheduler() {
        val interval = prefs.getInt("check_interval", 60)
        JobSchedulerUtil.scheduleAuthStateResetJob(this, interval)
        Toast.makeText(this, "Job Scheduler started", Toast.LENGTH_SHORT).show()
    }

    private fun stopJobScheduler() {
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as android.app.job.JobScheduler
        jobScheduler.cancel(JobSchedulerUtil.JOB_ID_AUTH_STATE_RESET)
        Toast.makeText(this, "Job Scheduler stopped", Toast.LENGTH_SHORT).show()
    }

    private fun setJobSchedulerInterval(interval: Int, showToast: (String) -> Unit) {
        Log.d("AppOptionsActivity", "setJobSchedulerInterval called with interval: $interval")
        if (interval in 15..1440) { // Adjusted for testing with shorter intervals
            with(prefs.edit()) {
                putInt("check_interval", interval)
                apply()
            }
            Log.d("AppOptionsActivity", "Interval saved to shared preferences")
            try {
                JobSchedulerUtil.scheduleAuthStateResetJob(this, interval)
                showToast("Interval set to $interval minutes")
            } catch (e: IllegalArgumentException) {
                Log.e("JobSchedulerUtil", "Error setting job scheduler interval", e)
                showToast("Failed to set interval: ${e.message}")
            }
        } else {
            showToast("Please enter a valid interval (15-1440 minutes)")
        }
    }

    private fun resetAuthenticationState() {
        AuthStateManager.resetAuthState(applicationContext)
    }

    private fun navigateToCriticalOptions() {
        lifecycleScope.launch {
            passwordChecker.checkAndRequestPassword(
                context = this@AppOptionsActivity,
                chosenApp = "critical_settings",
                onSuccess = {
                    startActivity(Intent(this@AppOptionsActivity, CriticalSettingsActivity::class.java))

                },
                onFailure = {
                    Toast.makeText(this@AppOptionsActivity, "Password check failed", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("AppOptionsActivity", "onActivityResult called with requestCode: $requestCode, resultCode: $resultCode")
        when (requestCode) {
            REQUEST_CODE_LOCK_SCREEN -> {
                if (resultCode == RESULT_OK) {
                    val passwordType = data?.getStringExtra("chosenApp")
                    Log.d("AppOptionsActivity", "REQUEST_CODE_LOCK_SCREEN returned RESULT_OK")
                    if (passwordType != null) {
                        Log.d("AppOptionsActivity", "Password type: $passwordType")
                        if (passwordType == "critical_settings") {
                            startActivity(Intent(this, CriticalSettingsActivity::class.java))
                        }
                    } else {
                        Log.d("AppOptionsActivity", "Password type is null")
                    }
                } else {
                    Log.d("AppOptionsActivity", "REQUEST_CODE_LOCK_SCREEN returned result code: $resultCode")
                    Toast.makeText(this, "Password verification failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}






