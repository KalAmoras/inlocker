package com.kalsys.inlocker

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PermissionManager(private val activity: ComponentActivity) {

    companion object {
//        private val DIRECTORY_URI_KEY = stringPreferencesKey("directory_uri")

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private val dataStoreManager = DataStoreManager(activity)

    // Registering result launchers for different permissions
    private val requestPermissionsLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            handlePermissionsResult(permissions)
            checkAndRequestPermissions()
            Log.d("PermissionManager", "Permissions requested, re-checking.")
        }

    private val overlayPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(activity)) {
                checkAndRequestPermissions()
                Log.d("PermissionManager", "Overlay permission granted.")
            } else {
                showToast("Overlay permission is required for the app to function correctly.")
                Log.d("PermissionManager", "Overlay permission not granted.")
            }
        }

    private val directoryAccessLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleDirectoryAccessResult(result)
            checkAndRequestPermissions()
        }

    private val accessibilityPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (isAccessibilityServiceEnabled()) {
                checkAndRequestPermissions()
                Log.d("PermissionManager", "Accessibility service is enabled.")
            } else {
                showToast("Accessibility permission is required for the app to function correctly.")
                Log.d("PermissionManager", "Accessibility permission not granted.")
            }
        }

    private fun handlePermissionsResult(permissions: Map<String, Boolean>) {
        permissions.entries.forEach { entry ->
            val permission = entry.key
            val isGranted = entry.value
            if (isGranted) {
                Log.d("PermissionManager", "$permission granted")
            } else {
                Log.d("PermissionManager", "$permission not granted")
                showToast("$permission is required for the app to function correctly. Please enable it.")
            }
        }
    }

    private fun handleDirectoryAccessResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                Log.d("PermissionManager", "Directory access granted: $uri")
                activity.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                saveUriToDataStore(uri.toString())
                checkAndRequestPermissions()
            } else {
                Log.d("PermissionManager", "Directory access URI is null")
                showToast("Please select a valid directory.")
            }
        } else {
            Log.d("PermissionManager", "Directory access was not granted.")
            showToast("Directory access is required for the app to function correctly.")
        }
    }

    fun checkAndRequestPermissions() {
        Log.d("PermissionManager", "Checking permissions...")
        val permissionsToCheck = mutableListOf<String>()

        if (!hasAllPermissions()) {
            Log.d("PermissionManager", "Not all required permissions are granted. Requesting required permissions.")
            permissionsToCheck.addAll(REQUIRED_PERMISSIONS)
        }

        if (!hasOverlayPermission()) {
            Log.d("PermissionManager", "Overlay permission is not granted. Requesting overlay permission.")
            requestOverlayPermission()
            return
        }

        if (!isAccessibilityServiceEnabled()) {
            Log.d("PermissionManager", "Accessibility service is not enabled. Requesting accessibility permission.")
            requestAccessibilityPermission()
            return
        }

        if (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) && !hasBackgroundStartPermissionInMIUI()) {
            Log.d("PermissionManager", "Background start permission in MIUI is not granted. Requesting permission.")
            requestBackgroundStartPermissionInMIUI()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            if (!hasDirectoryPermission()) {
                Log.d("PermissionManager", "Directory access permission is not granted. Requesting directory access.")
                requestDirectoryAccess()
                return@launch
            }

            if (permissionsToCheck.isNotEmpty()) {
                Log.d("PermissionManager", "Launching permission requests for: ${permissionsToCheck.joinToString()}")
                requestPermissionsLauncher.launch(permissionsToCheck.toTypedArray())
            } else {
                Log.d("PermissionManager", "All required permissions are granted.")
            }
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        accessibilityPermissionLauncher.launch(intent)
    }

    private fun requestDirectoryAccess() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        directoryAccessLauncher.launch(intent)
    }

    private fun requestBackgroundStartPermissionInMIUI() {
        try {
            val intent = Intent().apply {
                component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(fallbackIntent)
        }
    }

    private fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(activity)
        } else {
            true
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            activity.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val componentName = ComponentName(activity, AppMonitorService::class.java).flattenToString()
        return enabledServices.split(':').any { service ->
            service.equals(componentName, ignoreCase = true)
        }
    }

    private fun hasBackgroundStartPermissionInMIUI(): Boolean {
        try {
            val intent = Intent().apply {
                setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity")
            }
            val resolveInfo = activity.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo != null) {
                Log.d("PermissionManager", "MIUI permission screen is available. Background permission might not be granted.")
                return false
            }
        } catch (e: Exception) {
            Log.d("PermissionManager", "Error checking MIUI background permission: ${e.message}")
        }
        return true
    }

    private suspend fun hasDirectoryPermission(): Boolean {
        val persistedUri = getUriFromDataStore()
        val permissions = activity.contentResolver.persistedUriPermissions
        val hasPermission = permissions.any { it.uri.toString() == persistedUri && it.isReadPermission }

        if (hasPermission) {
            Log.d("PermissionManager", "Directory permission is already granted: $persistedUri")
        } else {
            Log.d("PermissionManager", "Directory permission is not granted.")
        }

        return hasPermission
    }

    private fun saveUriToDataStore(uri: String) {
        CoroutineScope(Dispatchers.IO).launch {
            dataStoreManager.saveFolderUri(uri)
            Log.d("PermissionManager", "URI saved to DataStore: $uri")
        }
    }

    private suspend fun getUriFromDataStore(): String? {
        return dataStoreManager.getFolderUri().first()
    }

    private fun showToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
}
