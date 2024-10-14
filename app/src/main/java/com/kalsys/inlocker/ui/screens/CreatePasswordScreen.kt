package com.kalsys.inlocker.ui.screens


import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kalsys.inlocker.PlaceholderTextHelper
import com.kalsys.inlocker.ui.components.PasswordTextField
import com.kalsys.inlocker.ui.theme.InLockerTheme

@Composable
fun CreatePasswordScreen(
    chosenApp: String?,
    isSettingDefaultPassword: Boolean,
    isSettingDefaultPasswordForAll: Boolean,
    onSavePassword: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    val placeholderText = PlaceholderTextHelper.getPlaceholderTextOnCreatePassword(chosenApp)
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        PasswordTextField(
            label = placeholderText,
            password = password,
            onPasswordChange = { password = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (password.isNotBlank()) {
                    onSavePassword(password)
                } else {
                    Toast.makeText(context, "Password cannot be blank", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Password")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreatePasswordPreview() {
    InLockerTheme {
        CreatePasswordScreen(
            chosenApp = {}.toString(),
            isSettingDefaultPassword = false,
            isSettingDefaultPasswordForAll = false,
            onSavePassword = {}
        )
    }
}
