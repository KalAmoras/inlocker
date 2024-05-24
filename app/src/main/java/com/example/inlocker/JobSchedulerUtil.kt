package com.example.inlocker

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build

object JobSchedulerUtil {
    private const val JOB_ID = 1

    fun scheduleServiceRestartJob(context: Context, intervalMinutes: Int = 60) {
        val componentName = ComponentName(context, RestartJobService::class.java)
        val jobInfoBuilder = JobInfo.Builder(JOB_ID, componentName)
            .setPersisted(true)
            .setPeriodic(intervalMinutes * 60 * 1000L)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            jobInfoBuilder.setMinimumLatency(intervalMinutes * 60 * 1000L)
        }

        val jobInfo = jobInfoBuilder.build()
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(jobInfo)
    }
}
