package de.pulselibre

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import android.widget.TextView

/** Stage line, optional file count and bar for the historical download; hidden while idle. Tag: history_sync_progress. */
internal class HistorySyncProgressView(context: Context) : LinearLayout(context) {
    private val palette = AppPalette.of(context)
    private val strings = Strings.of(context)
    private val titleView = TextView(context).apply { textSize = 14f; setTextColor(palette.ink); setTypeface(null, android.graphics.Typeface.BOLD) }
    private val detailView = TextView(context).apply { textSize = 12f; setTextColor(palette.muted) }
    private val barHolder = LinearLayout(context)

    init {
        tag = "history_sync_progress"
        orientation = VERTICAL
        val pad = (14 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)
        background = GradientDrawable().apply { setColor(palette.card); cornerRadius = 16 * resources.displayMetrics.density }
        addView(titleView)
        addView(detailView, LayoutParams(-1, -2).apply { topMargin = (4 * resources.displayMetrics.density).toInt() })
        addView(barHolder, LayoutParams(-1, (8 * resources.displayMetrics.density).toInt()).apply { topMargin = (8 * resources.displayMetrics.density).toInt() })
        show(HistorySyncProgress.IDLE)
    }

    fun show(progress: HistorySyncProgress) {
        if (progress.stage == HistorySyncStage.IDLE) { visibility = GONE; return }
        visibility = VISIBLE
        titleView.text = HistorySyncProgressText.title(strings, progress)
        val detail = HistorySyncProgressText.detail(strings, progress)
        detailView.text = detail.orEmpty(); detailView.visibility = if (detail == null) GONE else VISIBLE
        barHolder.removeAllViews()
        val fraction = progress.fraction()
        if (fraction != null) {
            val color = if (progress.stage == HistorySyncStage.FAILED) palette.muted else palette.primary
            barHolder.addView(DeviceBatteryBar(context, (fraction * 100).toInt().coerceAtMost(100), color, palette.track), LayoutParams(-1, -1))
        }
        contentDescription = listOfNotNull(titleView.text, detail).joinToString(". ")
    }
}
