package com.kalsys.inlocker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kalsys.inlocker.PlaceholderTextHelper
import com.kalsys.inlocker.ui.components.CustomButton
import com.kalsys.inlocker.ui.components.PasswordTextField
import com.kalsys.inlocker.ui.theme.InLockerTheme


@Composable
fun LockScreen(
    appPackageName: String,
    onUnlockClicked: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    val placeholderText = PlaceholderTextHelper.getPlaceholderText(appPackageName)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PasswordTextField(
            label = placeholderText,
            password = password,
            onPasswordChange = { password = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        CustomButton(
            text = "Unlock",
            onClick = {
                onUnlockClicked(password)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LockScreenPreview() {
    InLockerTheme {
        LockScreen(
            appPackageName = "com.example.app",
            onUnlockClicked = {}
        )
    }
}