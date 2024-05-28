package com.example.inlocker

import AppListAdapter
import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.widget.SearchView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListActivity : AppCompatActivity() {

    private lateinit var appListRecyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter
    private lateinit var passwordDao: PasswordDao

    private val REQUEST_CODE_PASSWORD = 100


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.app_list_layout)

        appListRecyclerView = findViewById(R.id.appRecyclerView)
        appListRecyclerView.layoutManager = LinearLayoutManager(this)

        val passwordDatabase = PasswordDatabase.getInstance(applicationContext)
        passwordDao = passwordDatabase.passwordDao()

        displayInstalledApps()
        setupSearchView()

        CoroutineScope(Dispatchers.IO).launch {
            val passwordList = passwordDao.getAllPasswords()
            withContext(Dispatchers.Main) {
                for (passwordItem in passwordList) {
                    Log.d("AppListActivity", "App: ${passwordItem.chosenApp}, Password: ${passwordItem.password}")
                }
            }
        }
    }

    private fun displayInstalledApps() {
        val installedApps = getAllInstalledApps()
        if (installedApps.isNotEmpty()) {
            adapter = AppListAdapter(this, installedApps, null)
            appListRecyclerView.adapter = adapter
        } else {
            Toast.makeText(this, "No apps found.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAllInstalledApps(): List<ApplicationInfo> {
        val packageManager = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
        val appInfoList = mutableListOf<ApplicationInfo>()
        for (resolveInfo in resolveInfoList) {
            appInfoList.add(resolveInfo.activityInfo.applicationInfo)
        }
        return appInfoList
    }

    private fun setupSearchView() {
        val searchView: SearchView = findViewById(R.id.appSearchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
            }
        })
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
        if (requestCode == REQUEST_CODE_PASSWORD && resultCode == Activity.RESULT_OK) {
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
        }
    }
}
