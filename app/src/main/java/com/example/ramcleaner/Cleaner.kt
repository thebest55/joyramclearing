package com.example.ramcleaner

import android.app.ActivityManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("ramcleaner", Context.MODE_PRIVATE)

    fun whitelist(c: Context): Set<String> =
        sp(c).getStringSet("whitelist", emptySet()) ?: emptySet()

    fun setWhitelist(c: Context, s: Set<String>) {
        sp(c).edit().putStringSet("whitelist", HashSet(s)).apply()
    }

    fun interval(c: Context): Int = sp(c).getInt("interval", 0)

    fun setInterval(c: Context, minutes: Int) {
        sp(c).edit().putInt("interval", minutes).apply()
    }
}

object Cleaner {
    private const val JOB_ID = 4711

    data class Result(val count: Int, val beforeMb: Long, val afterMb: Long)

    private fun memInfo(c: Context): ActivityManager.MemoryInfo {
        val am = c.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return mi
    }

    fun availMb(c: Context): Long = memInfo(c).availMem / (1024 * 1024)
    fun totalMb(c: Context): Long = memInfo(c).totalMem / (1024 * 1024)

    /** Korunanlar hariç tüm uygulamaların arka plan işlemlerini kapatır. */
    fun killBackground(c: Context): Int {
        val am = c.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val protectedApps = Prefs.whitelist(c)

        val packages = c.packageManager
            .queryIntentActivities(launcher, 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != c.packageName && it !in protectedApps }

        var count = 0
        for (pkg in packages) {
            try {
                am.killBackgroundProcesses(pkg)
                count++
            } catch (e: Exception) {
            }
        }
        return count
    }

    /** Arka plan thread'inde çağırın (bekleme içerir). */
    fun cleanAndMeasure(c: Context, waitMs: Long): Result {
        val before = availMb(c)
        val count = killBackground(c)
        Thread.sleep(waitMs)
        val after = availMb(c)
        return Result(count, before, after)
    }

    /** minutes = 0 ise otomatik temizliği kapatır. En az 15 dakika. */
    fun schedule(c: Context, minutes: Int) {
        try {
            val js = c.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            js.cancel(JOB_ID)
            if (minutes <= 0) return
            val info = JobInfo.Builder(JOB_ID, ComponentName(c, AutoCleanJob::class.java))
                .setPeriodic(minutes * 60_000L)
                .setPersisted(true)
                .build()
            js.schedule(info)
        } catch (e: Exception) {
        }
    }
}
