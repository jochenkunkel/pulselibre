package de.pulselibre

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.view.MotionEvent
import android.view.VelocityTracker
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity

/**
 * Hidden test laboratory (#172, stage 1): five read-only areas A–E as tiles in one bottom-anchored dialog.
 * Opens by the hidden gesture only. Nothing here scans, connects or writes to the band; no trace is recorded.
 */
internal object DiagnosticsSheet {
    fun show(activity: AppCompatActivity) {
        val palette = AppPalette.of(activity)
        fun dp(v: Int) = (v * activity.resources.displayMetrics.density + 0.5f).toInt()
        fun text(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(activity).apply {
            text = t; textSize = size; setTextColor(color); if (bold) setTypeface(null, Typeface.BOLD)
        }
        val content = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(8)) }
        content.addView(text("Nur lesende Diagnose. Es wird nichts gescannt, verbunden oder aufgezeichnet.", 12f, palette.muted)
            .apply { setPadding(0, 0, 0, dp(10)) })

        val details = LinkedHashMap<String, TextView>()
        fun tile(key: String, icon: PlIcon, title: String, subtitle: String) {
            val body = text("Wird geladen …", 12f, palette.ink).apply {
                visibility = View.GONE; setPadding(dp(34), dp(6), 0, dp(6)); setTextIsSelectable(true); tag = "diagnostics_body_$key"
            }
            details[key] = body
            val header = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12)); minimumHeight = dp(52)
                background = GradientDrawable().apply { setColor(palette.tinted); cornerRadius = dp(14).toFloat() }
                isClickable = true; isFocusable = true; tag = "diagnostics_tile_$key"
                contentDescription = "$title. $subtitle. Tippen zum Ein- oder Ausklappen."
                addView(PlIcon.view(activity, icon, 24, palette.accent), LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(12) })
                val t = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
                t.addView(text(title, 15f, palette.ink, true).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO })
                t.addView(text(subtitle, 12f, palette.muted).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO })
                addView(t, LinearLayout.LayoutParams(0, -2, 1f))
                setOnClickListener { body.visibility = if (body.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
            }
            content.addView(header, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
            content.addView(body, LinearLayout.LayoutParams(-1, -2))
        }
        tile("app", PlIcon.INFO, "A · App-Diagnose", "Version, Android, Datenbank-Integrität")
        tile("bluetooth", PlIcon.SIGNAL, "B · Bluetooth-Diagnose", "Status und Berechtigungen, ohne Scan/Verbindung")
        tile("trace", PlIcon.SESSIONS, "C · Kommunikationsaufzeichnung", "Opt-in, begrenzt – noch nicht verfügbar")
        tile("sync", PlIcon.DATA, "D · Synchronisation", "FIT-Inventur und lokaler Datenbankabgleich")
        tile("export", PlIcon.STORAGE, "E · Diagnosepaket", "Vorschau, dann bewusst teilen")
        details.getValue("trace").text = DiagnosticsFormat.TRACE_PLACEHOLDER

        val share = text("Diagnosepaket teilen …", 14f, palette.accent, true).apply {
            setPadding(dp(34), dp(8), dp(12), dp(8)); isClickable = true; isFocusable = true; tag = "diagnostics_share"
            visibility = View.GONE
        }
        content.addView(share)
        val advanced = text("Erweitertes Testlabor (bisherige Ansicht)", 14f, palette.accent, true).apply {
            setPadding(dp(12), dp(16), dp(12), dp(8)); isClickable = true; isFocusable = true; tag = "diagnostics_advanced"
            minimumHeight = dp(48); gravity = Gravity.CENTER_VERTICAL
        }
        content.addView(advanced)
        val sciencePreview = text("Dashboard-Vorschau (synthetische Testdaten)", 14f, palette.accent, true).apply {
            setPadding(dp(12), dp(8), dp(12), dp(8)); isClickable = true; isFocusable = true; tag = "diagnostics_science_preview"
            minimumHeight = dp(48); gravity = Gravity.CENTER_VERTICAL
            setOnClickListener { ScienceDashboardPreview.show(activity) }
        }
        content.addView(sciencePreview)

        val maxScroll = (activity.resources.displayMetrics.heightPixels * 0.8f).toInt()
        val scroll = object : ScrollView(activity) {
            override fun onMeasure(w: Int, h: Int) =
                super.onMeasure(w, View.MeasureSpec.makeMeasureSpec(maxScroll, View.MeasureSpec.AT_MOST))
        }.apply { addView(content) }
        val sheet = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val closeLabel = text("Schließen", 14f, palette.accent, true).apply {
            setPadding(dp(16), dp(12), dp(16), dp(12)); minimumHeight = dp(48); gravity = Gravity.CENTER_VERTICAL
            isClickable = true; isFocusable = true; tag = "diagnostics_close"
        }
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL; tag = "diagnostics_header"
            addView(View(activity).apply {
                background = GradientDrawable().apply { setColor(palette.muted); alpha = 120; cornerRadius = dp(2).toFloat() }
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(36), dp(4)).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(10); bottomMargin = dp(2) })
            val row = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(text("Testlabor", 18f, palette.ink, true).apply {
                setPadding(dp(16), dp(8), dp(8), dp(8)); androidx.core.view.ViewCompat.setAccessibilityHeading(this, true)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(closeLabel)
            addView(row)
        }
        sheet.addView(header)
        sheet.addView(scroll, LinearLayout.LayoutParams(-1, -2))
        val dialog = DialogProbe.track(AlertDialog.Builder(activity).setView(sheet).create())
        closeLabel.setOnClickListener { dialog.dismiss() }
        // Swipe down on the header (handle + title) drags the sheet and dismisses it past 1/4 of its height or on a fast fling.
        var downY = 0f
        var tracker: VelocityTracker? = null
        header.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downY = e.rawY; tracker = VelocityTracker.obtain().also { it.addMovement(e) }; true }
                MotionEvent.ACTION_MOVE -> {
                    tracker?.addMovement(e)
                    sheet.translationY = (e.rawY - downY).coerceAtLeast(0f); true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    tracker?.addMovement(e); tracker?.computeCurrentVelocity(1000)
                    val fling = (tracker?.yVelocity ?: 0f) > dp(900)
                    tracker?.recycle(); tracker = null
                    if (e.actionMasked == MotionEvent.ACTION_UP && (fling || sheet.translationY > sheet.height / 4f)) dialog.dismiss()
                    else sheet.animate().translationY(0f).setDuration(150).start()
                    true
                }
                else -> false
            }
        }
        advanced.setOnClickListener {
            dialog.dismiss()
            activity.startActivity(Intent(activity, CirqaTestLaboratoryActivity::class.java))
        }
        dialog.setOnShowListener {
            dialog.window?.apply {
                setGravity(Gravity.BOTTOM)
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                // Top-rounded sheet surface without the alert-dialog insets; slides in from the bottom.
                setBackgroundDrawable(GradientDrawable().apply {
                    setColor(ContextCompat.getColor(activity, R.color.pl_card))
                    cornerRadii = floatArrayOf(dp(24).toFloat(), dp(24).toFloat(), dp(24).toFloat(), dp(24).toFloat(), 0f, 0f, 0f, 0f)
                })
                setWindowAnimations(android.R.style.Animation_InputMethod)
            }
        }
        dialog.show()

        Thread {
            val snapshot = DiagnosticsCollector.collect(activity)
            val stamp = java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            val report = DiagnosticsFormat.exportPackage(snapshot, stamp)
            activity.runOnUiThread {
                if (!dialog.isShowing) return@runOnUiThread
                details.getValue("app").text = DiagnosticsFormat.app(snapshot)
                details.getValue("bluetooth").text = DiagnosticsFormat.bluetooth(snapshot)
                details.getValue("sync").text = DiagnosticsFormat.sync(snapshot)
                details.getValue("export").text = "Vorschau (so wird es geteilt):\n\n$report"
                share.visibility = View.VISIBLE
                share.setOnClickListener {
                    activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, report)
                    }, "Diagnosepaket teilen"))
                }
            }
        }.start()
    }
}
