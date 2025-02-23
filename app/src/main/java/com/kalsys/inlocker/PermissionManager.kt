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

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }


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


    private fun showToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
}
