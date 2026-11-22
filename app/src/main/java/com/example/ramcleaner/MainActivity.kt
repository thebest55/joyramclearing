// NOT: Dosyanın en üstündeki package satırını kendi projenizin paket adıyla değiştirin.
package com.example.ramcleaner

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var infoText: TextView
    private lateinit var cleanButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        infoText = TextView(this).apply {
            textSize = 20f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        cleanButton = Button(this).apply {
            text = "RAM'i Temizle"
            textSize = 18f
            setOnClickListener { cleanRam() }
        }

        root.addView(infoText)
        root.addView(cleanButton)
        setContentView(root)

        showMemory("Mevcut durum")
    }

    private fun memoryInfo(): ActivityManager.MemoryInfo {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    }

    private fun showMemory(title: String) {
        val mi = memoryInfo()
        val freeMb = mi.availMem / (1024 * 1024)
        val totalMb = mi.totalMem / (1024 * 1024)
        infoText.text = "$title\nBoş RAM: $freeMb MB / $totalMb MB"
    }

    private fun cleanRam() {
        cleanButton.isEnabled = false
        infoText.text = "Temizleniyor..."

        thread {
            val before = memoryInfo().availMem / (1024 * 1024)

            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

            // Başlatılabilir (kullanıcı) uygulamaların paket adları
            val packages = packageManager
                .queryIntentActivities(launcherIntent, 0)
                .map { it.activityInfo.packageName }
                .distinct()
                .filter { it != packageName }

            var count = 0
            for (pkg in packages) {
                try {
                    am.killBackgroundProcesses(pkg)
                    count++
                } catch (e: Exception) {
                    // Bazı uygulamalar için hata verebilir, devam et
                }
            }

            Thread.sleep(1500) // Sistemin belleği geri toplaması için bekle

            val after = memoryInfo().availMem / (1024 * 1024)
            val diff = after - before

            runOnUiThread {
                infoText.text = "$count uygulama arka planda kapatıldı\n" +
                        "Önce: $before MB  →  Sonra: $after MB\n" +
                        "Kazanç: ${if (diff > 0) diff else 0} MB"
                cleanButton.isEnabled = true
            }
        }
    }
}
