package com.kalsys.inlocker

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log

class AuthStateResetJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d("AuthStateResetJobService", "Job started")
        AuthStateManager.resetAuthState(applicationContext)
        jobFinished(params, false)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d("AuthStateResetJobService", "Job stopped")
        return true
    }
}