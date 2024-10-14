package com.kalsys.inlocker.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.kalsys.inlocker.EmailInstructionActivity
import com.kalsys.inlocker.ui.components.CustomButton
import com.kalsys.inlocker.ui.theme.InLockerTheme

@Composable
fun EmailSettingsScreen(
    onSetRecoveryEmail: () -> Unit,
    onSendTestEmail: () -> Unit,
    onSendPasswordsEmail: () -> Unit,
) {
    val context = LocalContext.current


    Row(
        modifier = Modifier.padding(bottom = 10.dp)
    ){
        Text(
            "Email Settings",
            fontSize = 34.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        )
    }
    Row( horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = {
                val intent = Intent(context, EmailInstructionActivity::class.java)
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
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,

        ) {

        Text(
            "This email will receive the list of passwords and the data sent by InLocker",
            fontSize = 16.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        )
        CustomButton(
            text = "Set Recovery Email",
            onClick = onSetRecoveryEmail,
            modifier = Modifier
                .height(56.dp)
                .width(142.dp),
            shape = RoundedCornerShape(6.dp)
        )
        Spacer(modifier = Modifier. height(10.dp))
        Text(
            "Send a test email to verify if your account is receiving emails from InLocker",
            fontSize = 16.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        )
        CustomButton(
            text = "Send Test Email",
            onClick = onSendTestEmail,
            modifier = Modifier
                .height(56.dp)
                .width(142.dp),
            shape = RoundedCornerShape(6.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Send the list of saved passwords to your recovery email",
            fontSize = 16.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        )
        CustomButton(
            text = "Send Passwords Email",
            onClick = onSendPasswordsEmail,
            modifier = Modifier
                .height(56.dp)
                .width(142.dp),
            shape = RoundedCornerShape(6.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EmailSettingsPreview() {
    InLockerTheme {
        EmailSettingsScreen(
            onSetRecoveryEmail = {},
            onSendTestEmail = {},
            onSendPasswordsEmail = {},
        )
    }
}