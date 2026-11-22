package de.pulselibre

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.time.Instant
import java.util.concurrent.Executors

/**
 * The visible science dashboard (#45): real series from `unified_measurements`, one card per measurement with source, time
 * basis, quality marks and gaps; measurements without a verified data basis appear as explained empty states. No value is
 * derived from unestablished fields, nothing is interpolated.
 */
internal class ScienceDashboardView(context: Context) : LinearLayout(context) {
    private val palette = AppPalette.of(context)
    private val main = Handler(Looper.getMainLooper())
    private val density = resources.displayMetrics.density
    private fun dp(v: Int) = (v * density + 0.5f).toInt()
    private val cards = LinearLayout(context).apply { orientation = VERTICAL }
    private val rangeRow = LinearLayout(context).apply { orientation = HORIZONTAL }
    private var selected = 1 // 7 days: the band history usually lies days back
    private var generation = 0
    private val ranges = listOf(R.string.sd_r24 to 24L, R.string.sd_r7 to 168L, R.string.sd_r30 to 720L)
    private val specs = listOf(
        R.string.sd_hr_band to ScienceSeriesLoader.HEART_RATE_BAND, R.string.sd_hr_live to ScienceSeriesLoader.HEART_RATE_LIVE,
        R.string.sd_spo2 to ScienceSeriesLoader.SPO2_LIVE, R.string.sd_resp to ScienceSeriesLoader.RESPIRATION_LIVE,
        R.string.sd_stress to ScienceSeriesLoader.STRESS_LIVE)

    init {
        orientation = VERTICAL; tag = "science_dashboard"
        addView(label(context.getString(R.string.sd_title), 18f, palette.ink, true).apply { setPadding(0, dp(4), 0, 0); isFocusable = true })
        addView(label(context.getString(R.string.sd_intro), 12f, palette.muted), LayoutParams(-1, -2).apply { topMargin = dp(2); bottomMargin = dp(8) })
        ranges.forEachIndexed { i, (res, _) ->
            rangeRow.addView(label(context.getString(res), 12f, palette.accent, true).apply {
                tag = "science_range_$i"; gravity = Gravity.CENTER; minHeight = dp(48); isClickable = true; isFocusable = true
                background = GradientDrawable().apply { cornerRadius = dp(12).toFloat() }
                setOnClickListener { if (selected != i) { selected = i; refresh() } }
            }, LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        }
        addView(rangeRow, LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        addView(cards)
        refresh()
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false) = TextView(context).apply {
        this.text = text; textSize = size; setTextColor(color); if (bold) setTypeface(null, Typeface.BOLD)
    }

    private fun card(title: String, vararg views: View) = LinearLayout(context).apply {
        orientation = VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12))
        background = GradientDrawable().apply { setColor(palette.card); cornerRadius = dp(18).toFloat() }
        addView(label(title, 15f, palette.ink, true).apply { isFocusable = true })
        views.forEach { addView(it, LayoutParams(-1, -2).apply { topMargin = dp(6) }) }
    }

    /** Reloads every card on a worker thread; a stale result (range changed meanwhile) is dropped. */
    fun refresh() {
        val ticket = ++generation
        val hours = ranges[selected].second
        ranges.indices.forEach { i ->
            (rangeRow.getChildAt(i) as TextView).apply {
                isSelected = i == selected
                setTextColor(if (i == selected) palette.card else palette.accent)
                (background as GradientDrawable).setColor(if (i == selected) palette.primary else palette.tinted)
                contentDescription = context.getString(ranges[i].first) + if (i == selected) ", ✓" else ""
            }
        }
        cards.removeAllViews()
        cards.addView(card(context.getString(R.string.sd_title), label(context.getString(R.string.sd_loading), 13f, palette.muted)).apply { tag = "science_loading" })
        EXECUTOR.execute {
            val end = System.currentTimeMillis()
            val start = end - hours * 3_600_000L
            val results = try {
                CirqaDatabase(context.applicationContext).use { db ->
                    db.ensureHistoricalStoreCopied()
                    val unified = UnifiedMeasurements(db.writableDatabase)
                    unified.refresh()
                    val observations = HistoricalObservationRepository(context.applicationContext)
                    specs.map { (title, spec) ->
                        val data = if (spec.key == ScienceSeriesLoader.HEART_RATE_BAND.key) {
                            // unified_measurements is a display projection without FIT quality or timestamp-origin fields.
                            ScienceFitSeriesAdapter.readVerifiedSeries(
                                observations, ScienceFitSeriesAdapter.Metric.HEART_RATE, start, end + 1
                            )
                        } else {
                            ScienceSeriesLoader.load(db.readableDatabase, spec, start, end)
                        }
                        Triple(title, spec, data)
                    }
                }
            } catch (_: Exception) { null }
            main.post { if (ticket == generation) render(results) }
        }
    }

    private fun render(results: List<Triple<Int, ScienceSeriesSpec, SeriesData>>?) {
        cards.removeAllViews()
        if (results == null) {
            cards.addView(card(context.getString(R.string.sd_title), label(context.getString(R.string.sd_error), 13f, palette.muted)).apply { tag = "science_error" }, spacing())
        } else results.forEach { (titleRes, spec, data) ->
            val title = context.getString(titleRes)
            val a = SeriesAnalysis.assess(data)
            val basis = label(context.getString(if (spec.origin == MeasurementOrigin.BAND) R.string.sd_src_band else R.string.sd_src_live), 11f, palette.indicator, true)
            val caption = summary(a, spec)
            val body: View = when (a.state) {
                SeriesState.EMPTY -> label(context.getString(R.string.sd_empty), 13f, palette.muted)
                SeriesState.NO_VALID_VALUES -> label(context.getString(R.string.sd_only_invalid) + "\n" + caption, 13f, palette.muted)
                else -> ScienceSeriesView(context, palette).apply { show(data, context.getString(R.string.sd_cd, title, caption)) }
            }
            val views = ArrayList<View>().apply { add(basis); add(body); if (a.state != SeriesState.EMPTY && body is ScienceSeriesView) add(label(caption, 12f, palette.muted)) }
            cards.addView(card(title, *views.toTypedArray()).apply { tag = "science_card_${spec.key}" }, spacing())
        }
        val na = listOf(R.string.sd_na_sleep, R.string.sd_na_activity, R.string.sd_na_hrv, R.string.sd_na_temp)
        cards.addView(card(context.getString(R.string.sd_na_title), *na.map { label(context.getString(it), 13f, palette.muted) as View }.toTypedArray())
            .apply { tag = "science_not_available" }, spacing())
    }

    private fun spacing() = LayoutParams(-1, -2).apply { bottomMargin = dp(10) }

    private fun summary(a: SeriesAssessment, spec: ScienceSeriesSpec): String {
        val number = java.text.NumberFormat.getInstance(resources.configuration.locales[0]).apply { maximumFractionDigits = 1 }
        val coverage = Math.round(a.coverage * 100).toInt()
        val parts = ArrayList<String>()
        parts += if (spec.validQuality == SeriesQuality.VALID && a.valid > 0 && a.mean != null)
            context.getString(R.string.sd_summary, a.valid, number.format(a.mean), number.format(a.minimum ?: a.mean), number.format(a.maximum ?: a.mean), coverage)
        else context.getString(R.string.sd_summary_no_valid, a.total, coverage)
        if (a.invalid + a.status > 0) parts += context.getString(R.string.sd_flags, a.invalid, a.status)
        if (a.unverified > 0) parts += context.getString(R.string.sd_unverified)
        if (a.gaps.isNotEmpty()) parts += context.getString(R.string.sd_gaps, a.gaps.size)
        return parts.joinToString("\n")
    }

    private companion object { val EXECUTOR = Executors.newSingleThreadExecutor() }
}
