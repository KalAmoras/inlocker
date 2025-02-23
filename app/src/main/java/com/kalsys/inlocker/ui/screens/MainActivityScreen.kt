package com.kalsys.inlocker.ui.screens

import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalsys.inlocker.AppListActivity
import com.kalsys.inlocker.AppOptionsActivity
import com.kalsys.inlocker.InstructionActivity
import com.kalsys.inlocker.MainActivity
import com.kalsys.inlocker.R
//import com.kalsys.inlocker.UsbDeviceActivity
import com.kalsys.inlocker.ui.components.CustomButton
import com.kalsys.inlocker.ui.theme.InLockerTheme

@Composable
fun MainActivityScreen(
    appName: String,
    switchState: MutableState<Boolean>,
    sharedPreferences: SharedPreferences?,
    onEnableService: () -> Unit,
    onDisableService: () -> Unit
)
{
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (sharedPreferences != null) {
            switchState.value = sharedPreferences.getBoolean(MainActivity.MONITOR_SWITCH, false)
        }
    }

    Scaffold(modifier = Modifier
        .fillMaxSize()
    ) { innerPadding ->
        Row (
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ){
            Button(
                onClick = {
                    val intent = Intent(context, InstructionActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.padding(end = 12.dp)
                    .padding(top = 40.dp)
                    .width(40.dp)
                    .height(40.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp) // Remove default padding
            ) {
                Text(
                    text = "?",
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(top = 100.dp)
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.inlocker_logo),
                contentDescription = "Logo",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .width(120.dp)
                    .height(120.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {

                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (switchState.value) "Disable" else "Enable",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(
                    checked = switchState.value,
                    onCheckedChange = { isChecked ->
                        switchState.value = isChecked
                        if (isChecked) {
                            onEnableService()
                        } else {
                            onDisableService()
                        }
                        sharedPreferences?.edit()?.putBoolean(MainActivity.MONITOR_SWITCH, isChecked)
                            ?.apply()
                    },
                )

            }
            Spacer(modifier = Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 180.dp)
            ) {
                CustomButton(
                    onClick = {
                        val intent = Intent(context, AppListActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(8.dp),
                    text = "Set Apps Passwords"
                )
                CustomButton(
                    onClick = {
                        val intent = Intent(context, AppOptionsActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(8.dp),
                    text = "Options"
                )
//                CustomButton(
//                    onClick = {
//                        val intent = Intent(context, UsbDeviceActivity::class.java)
//                        context.startActivity(intent)
//                    },
//                    modifier = Modifier.padding(8.dp),
//                    text = "Show USB Devices"
//                )

            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MyAppContentPreview() {
    val dummySwitchState = remember { mutableStateOf(false) }

    InLockerTheme {
        MainActivityScreen(
            appName = "InLocker",
            switchState = dummySwitchState,
            sharedPreferences = null,
            onEnableService = { /* mock enable service */ },
            onDisableService = { /* mock disable service */}
        )
    }
}
