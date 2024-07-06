package com.kalsys.inlocker

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class AuthStateManager : JobService() {

    companion object {
        private const val PREFS_NAME = "auth_state_prefs"
        private var instance: AuthStateManager? = null

        fun getInstance(): AuthStateManager {
            if (instance == null) {
                instance = AuthStateManager()
            }
            return instance!!
        }

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
            Log.d("AuthStateManager", "Authentication state reset")
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d("AuthStateManager", "Job started")
        resetAuthState(applicationContext)
        jobFinished(params, false)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d("AuthStateManager", "Job stopped")
        return false
    }
}
