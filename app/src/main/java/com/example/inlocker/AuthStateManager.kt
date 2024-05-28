package com.example.inlocker

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object AuthStateManager {
    private const val PREFS_NAME = "auth_state_prefs"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAppAuthenticated(context: Context, packageName: String): Boolean {
        val prefs = getPreferences(context)
        return prefs.getBoolean(packageName, false)
    }

    fun setAppAuthenticated(context: Context, packageName: String) {
        val prefs = getPreferences(context)
        prefs.edit().putBoolean(packageName, true).apply()
        Log.d("AuthStateManager", "App authenticated")
    }

    fun resetAuthState(context: Context) {
        val prefs = getPreferences(context)
        prefs.edit().clear().apply()
        Log.d("AuthStateManager", "On InitializationService: Authentication state reset")
    }
}
