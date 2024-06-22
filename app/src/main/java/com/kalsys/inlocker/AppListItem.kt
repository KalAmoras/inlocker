package com.kalsys.inlocker

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter

@Composable
fun AppListItem(
    appInfo: ApplicationInfo,
    selectedPasswordItem: PasswordItem?,
    onSelectPassword: (ApplicationInfo) -> Unit
) {
    val packageManager = LocalContext.current.packageManager
    val appName = remember { appInfo.loadLabel(packageManager).toString() }
    val appIcon: Drawable = remember { appInfo.loadIcon(packageManager) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Image(
            painter = rememberAsyncImagePainter(appIcon),
            contentDescription = "App Icon",
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = appName, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Button(onClick = { onSelectPassword(appInfo) }) {
            Text(text = "Set Password")
        }
    }
}
