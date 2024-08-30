package com.kalsys.inlocker

import android.Manifest
import android.content.ActivityNotFoundException
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
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {
    companion object {
        private const val SYSTEM_ALERT_WINDOW_PERMISSION_CODE = 101
        private const val DIRECTORY_ACCESS_PERMISSION_CODE = 102
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    fun checkAndRequestPermissions(
        activity: ComponentActivity,
        requestPermissionsLauncher: ActivityResultLauncher<Array<String>>
    ) {
        if (!hasAllPermissions()) {
            requestPermissionsLauncher.launch(REQUIRED_PERMISSIONS)
        }

        if (!hasOverlayPermission()) {
            requestOverlayPermission(activity)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasAccessibilityPermission()) {
            requestAccessibilityPermission(activity)
        }

        if (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) && !hasBackgroundStartPermissionInMIUI(context)) {
            //TODO: Worked before, but the call for the overlay permission also shows this permission in the same menu
//            requestBackgroundStartPermissionInMIUI(activity)
        }
        //TODO: Breaks Accessibility permission call, needs working
//        requestDirectoryAccess(activity)
    }

    private fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    private fun requestOverlayPermission(activity: ComponentActivity) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        activity.startActivityForResult(intent, SYSTEM_ALERT_WINDOW_PERMISSION_CODE)
    }

    private fun requestDirectoryAccess(activity: ComponentActivity) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivityForResult(intent, DIRECTORY_ACCESS_PERMISSION_CODE)
    }

    private fun hasAccessibilityPermission(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        Log.d("PermissionManager", "Enabled Accessibility Services: $enabledServices")
        val componentName = ComponentName(context, AppMonitorService::class.java).flattenToString()
        return enabledServices.split(':').any { service ->
            service.equals(componentName, ignoreCase = true)
        }
    }

    private fun requestAccessibilityPermission(activity: ComponentActivity) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            activity.startActivityForResult(intent, SYSTEM_ALERT_WINDOW_PERMISSION_CODE)
        } catch (e: Exception) {
            Log.e("PermissionManager", "Error opening Accessibility Settings", e)
            Toast.makeText(context, "Unable to open Accessibility Settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasBackgroundStartPermissionInMIUI(context: Context): Boolean {
        val intent = Intent().apply {
            setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity")
        }
        val activityInfo = intent.resolveActivityInfo(context.packageManager, 0)
        return activityInfo?.exported ?: false
    }
    private fun requestBackgroundStartPermissionInMIUI(activity: ComponentActivity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        try {
            activity.startActivityForResult(intent, SYSTEM_ALERT_WINDOW_PERMISSION_CODE)
        } catch (e: Exception) {
            Log.e("PermissionManager", "Error opening App Settings", e)
            Toast.makeText(context, "Unable to open App Settings. Please manually enable the required permissions.", Toast.LENGTH_SHORT).show()
        }
    }


}
