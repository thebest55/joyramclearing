package com.example.ramcleaner

import android.app.job.JobParameters
import android.app.job.JobService

class AutoCleanJob : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        Thread {
            Cleaner.killBackground(applicationContext)
            jobFinished(params, false)
        }.start()
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = false
}
