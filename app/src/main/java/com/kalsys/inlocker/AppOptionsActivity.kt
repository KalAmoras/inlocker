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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontVariation.width
import androidx.compose.ui.unit.sp
import com.kalsys.inlocker.ui.components.CustomButton
import com.kalsys.inlocker.ui.components.PulsingSphere
import com.kalsys.inlocker.ui.theme.InLockerTheme

class AppOptionsActivity : AppCompatActivity() {

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        val intent = Intent(this, CriticalSettingsActivity::class.java)
        startActivity(intent)
    }
}


@Preview(showBackground = true)
@Composable
fun AppOptionsPreview() {
    InLockerTheme {
        AppOptionsScreen(
            onSetInterval = { _, _ -> },
            onResetAuthState = {},
            onNavigateToCriticalOptions = {},
            isJobScheduled = false,
            onToggleJobScheduler = {},
        )
    }
}


@Composable
fun AppOptionsScreen(
    onSetInterval: (Int, (String) -> Unit) -> Unit,
    onResetAuthState: () -> Unit,
    onNavigateToCriticalOptions: () -> Unit,
    isJobScheduled: Boolean,
    onToggleJobScheduler: () -> Unit
    ) {
    var intervalText by remember { mutableStateOf("") }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    toastMessage?.let { message ->
        ShowToast(message)
        toastMessage = null
    }
    Text(
        "Options",
        fontSize = 34.sp,
        lineHeight = 20.sp,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        Row(
            modifier = Modifier
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .height(140.dp)
                    .weight(1f)
            ) {
                Text(
                    "Reset the protection by resetting authentication",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                CustomButton(
                    text = "Reset Authentication",
                    onClick = onResetAuthState,
                    modifier = Modifier
                        .height(56.dp)
                        .width(142.dp),
                    shape = RoundedCornerShape(6.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .height(140.dp)
                    .weight(1f)
            ) {
                Text(
                    "Start authentication reset scheduling",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                CustomButton(
                    text = if (isJobScheduled) "Stop authentication reset scheduling" else "Start authentication reset scheduling",
                    onClick = onToggleJobScheduler,
                    modifier = Modifier
                        .height(56.dp)
                        .width(142.dp),
                    shape = RoundedCornerShape(6.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .weight(0.8f)
                .padding(top = 10.dp),
        ) {
            Text(
                "Set an interval for the authentication reset",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedTextField(
                value = intervalText,
                onValueChange = { intervalText = it },
                label = { Text("Set Interval in minutes (min: 15, max: 1440") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            CustomButton(
                text = "Set Interval",
                onClick = {
                    val interval = intervalText.toIntOrNull()
                    if (interval != null) {
                        onSetInterval(interval) { message ->
                            toastMessage = message
                        }
                    } else {
                        toastMessage = "Please enter a valid interval (15-1440 minutes)"
                    }
                },
                modifier = Modifier
                    .height(56.dp)
                    .width(142.dp),
                shape = RoundedCornerShape(6.dp)
            )
        }

//        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(20.dp)
        ) {

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Color.LightGray.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .blur(10.dp, BlurredEdgeTreatment(RoundedCornerShape(10.dp)))
            )
            PulsingSphere(
                modifier = Modifier.align(Alignment.Center)
            )
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CustomButton(
                    text = "Go to Critical Options",
                    onClick = onNavigateToCriticalOptions,
                    modifier = Modifier
                        .height(56.dp)
                        .width(152.dp),
                    shape = RoundedCornerShape(6.dp)
                )
            }
        }

    }
}

