package com.kalsys.inlocker

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import com.kalsys.inlocker.ui.theme.InLockerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListActivity : AppCompatActivity() {

    private lateinit var passwordDao: PasswordDao
    private val REQUEST_CODE_PASSWORD = 100
    private lateinit var installedApps: List<ApplicationInfo>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InLockerTheme {
                val context = LocalContext.current
                passwordDao = PasswordDatabase.getInstance(context).passwordDao()
                installedApps = getAllInstalledApps()

                var filteredApps by remember { mutableStateOf(installedApps) }
                var selectedPasswordItem by remember { mutableStateOf<PasswordItem?>(null) }

                LaunchedEffect(Unit) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val passwordList = passwordDao.getAllPasswords()
                        withContext(Dispatchers.Main) {
                            for (passwordItem in passwordList) {
                                Log.d("AppListActivity", "App: ${passwordItem.chosenApp}, Password: ${passwordItem.password}")
                            }
                        }
                    }
                }

                AppListScreen(
                    apps = filteredApps,
                    selectedPasswordItem = selectedPasswordItem,
                    onSelectPassword = { appInfo ->
                        val chosenAppPackageName = appInfo.packageName
                        val intent = Intent(context, CreatePasswordActivity::class.java)
                        intent.putExtra("chosenApp", chosenAppPackageName)
                        startActivityForResult(intent, REQUEST_CODE_PASSWORD)
                    },
                    onSearch = { query ->
                        filteredApps = if (query.isEmpty()) {
                            installedApps
                        } else {
                            installedApps.filter {
                                it.loadLabel(packageManager).toString().contains(query, ignoreCase = true)
                            }
                        }
                    },
                    onSetDefaultPassword = {
                        val intent = Intent(context, CreatePasswordActivity::class.java)
                        intent.putExtra("setDefaultPasswordForAll", true)
                        startActivityForResult(intent, REQUEST_CODE_PASSWORD)
                    }
                )
            }
        }
    }


    private fun getAllInstalledApps(): List<ApplicationInfo> {
        val packageManager = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return packageManager.queryIntentActivities(intent, 0)
            .map { it.activityInfo.applicationInfo }
    }


    fun onSelectPasswordButtonClick(view: View) {
        val appInfo = view.tag as? ApplicationInfo

        if (appInfo != null) {
            val chosenAppPackageName = appInfo.packageName
            val intent = Intent(this, CreatePasswordActivity::class.java)
            intent.putExtra("chosenApp", chosenAppPackageName)
            startActivityForResult(intent, REQUEST_CODE_PASSWORD)
        } else {
            Log.e("AppListActivity", "ApplicationInfo is null for the selected app")
            Toast.makeText(this, "Error: ApplicationInfo is null for the selected app", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PASSWORD) {
            if (resultCode == Activity.RESULT_OK) {
                val chosenAppPackageName = data?.getStringExtra("chosenApp")
                val newPassword = data?.getStringExtra("newPassword")
                Log.d("AppListActivity", "Received result for $chosenAppPackageName with new password")

                if (!chosenAppPackageName.isNullOrEmpty() && !newPassword.isNullOrEmpty()) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                passwordDao.insertOrUpdate(PasswordItem(chosenAppPackageName, newPassword))
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@AppListActivity, "Password created successfully", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("AppListActivity", "Error updating password", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@AppListActivity, "Error: Unable to create password", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Error: Unable to create password", Toast.LENGTH_SHORT).show()
                }
            } else if (resultCode == Activity.RESULT_OK && data?.getBooleanExtra("setDefaultPasswordForAll", false) == true) {
                Toast.makeText(this, "Default password set for all apps", Toast.LENGTH_SHORT).show()
            }
        }
    }


    @Composable
    fun AppListScreen(
        apps: List<ApplicationInfo>,
        selectedPasswordItem: PasswordItem?,
        onSelectPassword: (ApplicationInfo) -> Unit,
        onSearch: (String) -> Unit,
        onSetDefaultPassword: () -> Unit
    ) {
        var query by remember { mutableStateOf("") }

        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "App List",
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp)
            )
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
            Button(
                onClick = onSetDefaultPassword,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(text = "Set Default Password for All Apps")
            }
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

}
