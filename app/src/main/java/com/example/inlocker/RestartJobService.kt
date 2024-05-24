package com.example.inlocker

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent

class RestartJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {

        val serviceIntent = Intent(applicationContext, AppMonitorService::class.java)
        applicationContext.startForegroundService(serviceIntent)

        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true
    }
}
