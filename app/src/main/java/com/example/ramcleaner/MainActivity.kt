package com.example.ramcleaner

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private val cBg = Color.parseColor("#0F172A")
    private val cCard = Color.parseColor("#1E293B")
    private val cGreen = Color.parseColor("#22C55E")
    private val cGray = Color.parseColor("#94A3B8")
    private val cBtn = Color.parseColor("#334155")

    private lateinit var percentText: TextView
    private lateinit var detailText: TextView
    private lateinit var resultText: TextView
    private lateinit var bar: ProgressBar
    private lateinit var cleanBtn: Button
    private lateinit var whitelistBtn: Button
    private lateinit var autoBtn: Button

    private val intervalLabels = arrayOf("Kapalı", "Her 15 dakika", "Her 30 dakika", "Her 1 saat", "Her 2 saat")
    private val intervalValues = intArrayOf(0, 15, 30, 60, 120)

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable {
        val d = GradientDrawable()
        d.setColor(color)
        d.cornerRadius = dp(radiusDp).toFloat()
        return d
    }

    private fun lp(top: Int = 0, height: Int = WRAP): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(MATCH, height)
        p.topMargin = dp(top)
        return p
    }

    private fun text(label: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = label
        t.textSize = size
        t.setTextColor(color)
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        return t
    }

    private fun makeButton(label: String, fill: Int, textColor: Int, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = label
        b.isAllCaps = false
        b.textSize = 16f
        b.typeface = Typeface.DEFAULT_BOLD
        b.setTextColor(textColor)
        b.background = rounded(fill, 16)
        b.setOnClickListener { onClick() }
        return b
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = cBg
        window.navigationBarColor = cBg

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(cBg)
        scroll.isFillViewport = true

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(20), dp(36), dp(20), dp(24))
        scroll.addView(root)

        root.addView(text("RAM Temizleyici", 26f, Color.WHITE, true), lp())
        root.addView(text("Arka plandaki uygulamaları tek dokunuşla kapat", 14f, cGray), lp(4))

        // RAM kartı
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = rounded(cCard, 20)
        card.setPadding(dp(20), dp(20), dp(20), dp(20))

        card.addView(text("RAM kullanımı", 14f, cGray), lp())
        percentText = text("--%", 44f, Color.WHITE, true)
        card.addView(percentText, lp(4))

        bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        bar.max = 100
        bar.progressTintList = ColorStateList.valueOf(cGreen)
        bar.progressBackgroundTintList = ColorStateList.valueOf(cBtn)
        card.addView(bar, lp(12, dp(10)))

        detailText = text("", 14f, cGray)
        card.addView(detailText, lp(12))
        root.addView(card, lp(24))

        // Temizle butonu
        cleanBtn = makeButton("Şimdi Temizle", cGreen, cBg) { cleanRam() }
        cleanBtn.textSize = 20f
        root.addView(cleanBtn, lp(20, dp(64)))

        resultText = text("", 15f, cGreen)
        resultText.gravity = android.view.Gravity.CENTER
        root.addView(resultText, lp(12))

        // Ayarlar
        root.addView(text("Ayarlar", 16f, Color.WHITE, true), lp(28))

        whitelistBtn = makeButton("", cBtn, Color.WHITE) { showWhitelist() }
        root.addView(whitelistBtn, lp(10, dp(52)))

        autoBtn = makeButton("", cBtn, Color.WHITE) { showAutoDialog() }
        root.addView(autoBtn, lp(10, dp(52)))

        root.addView(
            text(
                "Widget için: ana ekrana uzun basın → Widget'lar → RAM Temizleyici.\n" +
                    "Not: Android sistem kısıtları yüzünden yalnızca arka plandaki işlemler kapatılabilir.",
                12f, cGray
            ), lp(20)
        )

        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        refresh()
        updateButtons()
    }

    private fun refresh() {
        val total = Cleaner.totalMb(this)
        val avail = Cleaner.availMb(this)
        val used = total - avail
        val pct = if (total > 0) (used * 100 / total).toInt() else 0
        percentText.text = "$pct%"
        bar.progress = pct
        detailText.text = "Boş: $avail MB  /  Toplam: $total MB"
    }

    private fun updateButtons() {
        whitelistBtn.text = "Korunan uygulamalar (${Prefs.whitelist(this).size})"
        val idx = intervalValues.indexOf(Prefs.interval(this)).coerceAtLeast(0)
        autoBtn.text = "Otomatik temizlik: ${intervalLabels[idx]}"
    }

    private fun cleanRam() {
        cleanBtn.isEnabled = false
        cleanBtn.alpha = 0.6f
        cleanBtn.text = "Temizleniyor..."
        thread {
            val r = Cleaner.cleanAndMeasure(this, 1500)
            val gain = (r.afterMb - r.beforeMb).coerceAtLeast(0)
            runOnUiThread {
                resultText.text = "${r.count} uygulama kapatıldı  •  +$gain MB boşaldı"
                cleanBtn.text = "Şimdi Temizle"
                cleanBtn.alpha = 1f
                cleanBtn.isEnabled = true
                refresh()
            }
        }
    }

    private fun showWhitelist() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(launcher, 0)
            .map { Pair(it.activityInfo.packageName, it.loadLabel(packageManager).toString()) }
            .filter { it.first != packageName }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }

        val saved = Prefs.whitelist(this)
        val labels = apps.map { it.second }.toTypedArray()
        val checked = BooleanArray(apps.size) { apps[it].first in saved }

        AlertDialog.Builder(this)
            .setTitle("Kapatılmayacak uygulamalar")
            .setMultiChoiceItems(labels, checked) { _, i, isChecked -> checked[i] = isChecked }
            .setPositiveButton("Kaydet") { _, _ ->
                val set = apps.indices.filter { checked[it] }.map { apps[it].first }.toSet()
                Prefs.setWhitelist(this, set)
                updateButtons()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showAutoDialog() {
        val cur = intervalValues.indexOf(Prefs.interval(this)).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Otomatik temizlik")
            .setSingleChoiceItems(intervalLabels, cur) { dialog, which ->
                Prefs.setInterval(this, intervalValues[which])
                Cleaner.schedule(this, intervalValues[which])
                updateButtons()
                dialog.dismiss()
            }
            .show()
    }
}
