package com.kalsys.inlocker.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.kalsys.inlocker.OptionsInstructionActivity
import com.kalsys.inlocker.ShowToast
import com.kalsys.inlocker.ui.components.CustomButton
import com.kalsys.inlocker.ui.theme.InLockerTheme

@Composable
fun AppOptionsScreen(
    onSetInterval: (Int, (String) -> Unit) -> Unit,
    onResetAuthState: () -> Unit,
    onNavigateToCriticalOptions: () -> Unit,
    isJobScheduled: Boolean,
    onToggleJobScheduler: () -> Unit
) {
    val context = LocalContext.current

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
    Row( horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = {
                val intent = Intent(context, OptionsInstructionActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier.padding(end = 12.dp)
                .padding(top = 20.dp)
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
            //Activate if you're using Android 12+
//            PulsingSphere(
//                modifier = Modifier.align(Alignment.Center)
//            )
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

