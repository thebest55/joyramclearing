package com.example.ramcleaner

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import android.widget.Toast

class CleanWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_CLEAN = "com.example.ramcleaner.ACTION_CLEAN"
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            manager.updateAppWidget(id, buildViews(context))
        }
    }

    private fun buildViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget)
        val intent = Intent(context, CleanWidget::class.java).setAction(ACTION_CLEAN)
        val pi = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pi)
        return views
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CLEAN) {
            val pending = goAsync()
            val appContext = context.applicationContext
            Thread {
                val r = Cleaner.cleanAndMeasure(appContext, 1000)
                val gain = (r.afterMb - r.beforeMb).coerceAtLeast(0)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        appContext,
                        "${r.count} uygulama kapatıldı, +$gain MB",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                pending.finish()
            }.start()
        }
    }
}
