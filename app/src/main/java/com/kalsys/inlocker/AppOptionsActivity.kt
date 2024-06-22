package com.kalsys.inlocker

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import com.kalsys.inlocker.ui.components.CustomButton
import com.kalsys.inlocker.ui.theme.InLockerTheme

class AppOptionsActivity : AppCompatActivity() {

     override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            InLockerTheme {
                AppOptionsScreen(
                    onSetInterval = { interval -> setJobSchedulerInterval(interval) },
                    onResetAuthState = { resetAuthenticationState() },
                    onNavigateToCriticalOptions = { navigateToCriticalOptions() }
                )
            }
        }
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
            onSetInterval = { },
            onResetAuthState = {},
            onNavigateToCriticalOptions = {}

        )
    }
}


@Composable
fun AppOptionsScreen(
    onSetInterval: (Int) -> Unit,
    onResetAuthState: () -> Unit,
    onNavigateToCriticalOptions: () -> Unit
) {

//    var intervalText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Options",
            fontSize = 30.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onBackground

        )
        Spacer(modifier = Modifier.height(60.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .height(120.dp)
                    .weight(1f)

            ) {
                Text(
                    "Reset the protection by resetting authentication",
                    fontSize = 12.sp,
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
                    .height(120.dp)
                    .weight(1f)

            ) {

            }
        }

        Spacer(modifier = Modifier.weight(1f))
        CustomButton(
            text = "Go to Critical Options",
            onClick = onNavigateToCriticalOptions,
            modifier = Modifier
                .height(56.dp)
                .width(152.dp),
            shape = RoundedCornerShape(6.dp)
        )


//        OutlinedTextField(
//            value = intervalText,
//            onValueChange = { intervalText = it },
//            label = { Text("Set Interval") },
//            modifier = Modifier.fillMaxWidth()
//        )
//        CustomButton(
//            text = "Set Interval",
//            onClick = onSetInterval,
//            modifier = Modifier
//                .height(56.dp)
//                .width(142.dp),
//            shape = RoundedCornerShape(6.dp)
//        )

//        Spacer(modifier = Modifier.weight(1f))




    }
}

