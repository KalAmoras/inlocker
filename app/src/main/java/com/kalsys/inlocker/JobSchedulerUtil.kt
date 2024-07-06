package com.kalsys.inlocker

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log

object JobSchedulerUtil {
    const val JOB_ID_AUTH_STATE_RESET = 1

    fun scheduleAuthStateResetJob(context: Context, intervalMinutes: Int = 60) {
        val componentName = ComponentName(context, AuthStateManager::class.java)
        val jobInfoBuilder = JobInfo.Builder(JOB_ID_AUTH_STATE_RESET, componentName)
            .setPersisted(true)
            .setPeriodic(intervalMinutes * 60 * 1000L)

        try {
            val jobInfo = jobInfoBuilder.build()
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val result = jobScheduler.schedule(jobInfo)
            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.d("JobSchedulerUtil", "Job scheduled successfully with interval: $intervalMinutes minutes")
            } else {
                Log.e("JobSchedulerUtil", "Job scheduling failed with result code: $result")
            }
        } catch (e: IllegalArgumentException) {
            Log.e("JobSchedulerUtil", "Error setting job scheduler interval", e)
        }
    }
}
