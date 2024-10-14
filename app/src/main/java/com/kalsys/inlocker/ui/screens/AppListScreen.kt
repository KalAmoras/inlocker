package com.kalsys.inlocker.ui.screens

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalsys.inlocker.AppInstructionActivity
import com.kalsys.inlocker.AppListItem
import com.kalsys.inlocker.PasswordItem
import com.kalsys.inlocker.ui.components.CustomButton
import com.kalsys.inlocker.ui.theme.InLockerTheme

@Composable
fun AppListScreen(
    apps: List<ApplicationInfo>,
    selectedPasswordItem: PasswordItem?,
    onSelectPassword: (ApplicationInfo) -> Unit,
    onSearch: (String) -> Unit,
    onSetDefaultPassword: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current

    Row(
        modifier = Modifier.padding(bottom = 10.dp)
    ){
        Text(
            "App List",
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
                val intent = Intent(context, AppInstructionActivity::class.java)
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

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(top = 60.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            },
            label = { Text(text = "Search apps") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        CustomButton(
            onClick = onSetDefaultPassword,
            modifier = Modifier
                .padding(16.dp)
                .width(190.dp),
            text = "Set Default Password for All Apps"
        )
        LazyColumn {
            items(apps) { app ->
                AppListItem(appInfo = app, selectedPasswordItem = selectedPasswordItem, onSelectPassword = onSelectPassword)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppListScreenPreview() {
    InLockerTheme {
        AppListScreen(
            apps = emptyList(),
            selectedPasswordItem = null,
            onSelectPassword = {},
            onSearch = {},
            onSetDefaultPassword = {}
        )
    }
}