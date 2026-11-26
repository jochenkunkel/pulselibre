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
    @Volatile private var generation = 0
    private var inventory: CaptureInventory = CaptureInventory.EMPTY
    private val ranges = listOf(R.string.sd_r24 to 24L, R.string.sd_r7 to 168L, R.string.sd_r30 to 720L)
    private val specs = listOf(
        R.string.sd_hr_band to ScienceSeriesLoader.HEART_RATE_BAND,
        R.string.sd_hr_band_activity to ScienceSeriesLoader.HEART_RATE_BAND_ACTIVITY,
        R.string.sd_distance_band to ScienceSeriesLoader.DISTANCE_BAND, R.string.sd_speed_band to ScienceSeriesLoader.SPEED_BAND,
        R.string.sd_hr_live to ScienceSeriesLoader.HEART_RATE_LIVE,
        R.string.sd_spo2 to ScienceSeriesLoader.SPO2_LIVE, R.string.sd_resp to ScienceSeriesLoader.RESPIRATION_LIVE,
        R.string.sd_stress to ScienceSeriesLoader.STRESS_LIVE,
        R.string.sd_spo2_band to ScienceSeriesLoader.SPO2_BAND, R.string.sd_resp_band to ScienceSeriesLoader.RESPIRATION_BAND,
        R.string.sd_stress_band to ScienceSeriesLoader.STRESS_BAND)

    /**
     * Band cards and the FIT message kind they show (data source matrix, docs/product/ui/DATENQUELLEN_DIAGRAMME.md):
     * all-day history (monitoring) and recorded activities (record) are separate cards; distance and speed exist only
     * in activity records.
     */
    private val fitMetrics = mapOf(
        ScienceSeriesLoader.HEART_RATE_BAND.key to (ScienceFitSeriesAdapter.Metric.HEART_RATE to BandRecordKind.MONITORING),
        ScienceSeriesLoader.HEART_RATE_BAND_ACTIVITY.key to (ScienceFitSeriesAdapter.Metric.HEART_RATE to BandRecordKind.ACTIVITY),
        ScienceSeriesLoader.DISTANCE_BAND.key to (ScienceFitSeriesAdapter.Metric.DISTANCE to BandRecordKind.ACTIVITY),
        ScienceSeriesLoader.SPEED_BAND.key to (ScienceFitSeriesAdapter.Metric.SPEED to BandRecordKind.ACTIVITY))

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
            // A6-63: taps on the range buttons queue one heavy load each; a load whose result would be dropped is not started.
            if (ticket != generation) return@execute
            val end = System.currentTimeMillis()
            val start = end - hours * 3_600_000L
            var loadedInventory = CaptureInventory.EMPTY
            val results = try {
                CirqaDatabase(context.applicationContext).use { db ->
                    db.ensureHistoricalStoreCopied()
                    loadedInventory = CaptureInventory.load(db.readableDatabase)
                    val unified = UnifiedMeasurements(db.writableDatabase)
                    unified.refresh()
                    val observations = HistoricalObservationRepository(context.applicationContext)
                    // One read per FIT metric, shared by its all-day and activity cards.
                    val fitRead = HashMap<ScienceFitSeriesAdapter.Metric, List<HistoricalObservation>>()
                    specs.map { (title, spec) ->
                        if (ticket != generation) throw java.util.concurrent.CancellationException("superseded")
                        val fit = fitMetrics[spec.key]
                        val data = if (fit != null) {
                            // unified_measurements is a display projection without FIT quality or timestamp-origin fields.
                            val (metric, kind) = fit
                            val rows = fitRead.getOrPut(metric) {
                                ScienceFitSeriesAdapter.readVerifiedObservations(observations, metric, start, end + 1)
                            }
                            ScienceFitSeriesAdapter.verifiedSeries(metric, rows, start, end + 1, kind)
                        } else {
                            ScienceSeriesLoader.load(db.readableDatabase, spec, start, end)
                        }
                        Triple(title, spec, data)
                    }
                }
            } catch (_: Exception) { null }
            // A6-66: a very large range must end in the error card, not in an unhandled error on the worker thread.
            catch (_: OutOfMemoryError) { null }
            main.post { if (ticket == generation) { inventory = loadedInventory; render(results) } }
        }
    }

    private fun render(results: List<Triple<Int, ScienceSeriesSpec, SeriesData>>?) {
        cards.removeAllViews()
        // Capture status first (P0: data completeness before charts); it is independent of the selected range and of any series.
        cards.addView(captureCard(inventory), spacing())
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
            val views = ArrayList<View>().apply {
                add(basis); add(body)
                if (a.state != SeriesState.EMPTY && body is ScienceSeriesView) {
                    add(label(caption, 12f, palette.muted))
                    sparseDaysHint(data)?.let { add(label(it, 12f, palette.muted).apply { tag = "science_days_hint" }) }
                }
            }
            cards.addView(card(title, *views.toTypedArray()).apply { tag = "science_card_${spec.key}" }, spacing())
        }
        val na = listOf(R.string.sd_na_sleep, R.string.sd_na_activity, R.string.sd_na_hrv, R.string.sd_na_temp)
        cards.addView(card(context.getString(R.string.sd_na_title), *na.map { label(context.getString(it), 13f, palette.muted) as View }.toTypedArray())
            .apply { tag = "science_not_available" }, spacing())
    }

    /** Known candidate metrics get a localized name; an unknown key stays visible as stored, never dropped. */
    private fun candidateName(key: String): String = when (key) {
        "spo2" -> context.getString(R.string.sd_cap_m_spo2)
        "stress" -> context.getString(R.string.sd_cap_m_stress)
        "respiration" -> context.getString(R.string.sd_cap_m_respiration)
        else -> key
    }

    /** Capture counters only (P0): what is stored, never a health value and never a completeness claim. */
    private fun captureCard(inv: CaptureInventory): View {
        val lines = ArrayList<View>()
        lines += label(context.getString(R.string.sd_cap_intro), 12f, palette.muted)
        if (inv.isUnknown) lines += label(context.getString(R.string.sd_cap_unknown), 13f, palette.muted)
        else if (inv.isEmpty) lines += label(context.getString(R.string.sd_cap_none), 13f, palette.muted)
        else {
            inv.archivedFiles?.let { lines += label(context.getString(R.string.sd_cap_files, it, android.text.format.Formatter.formatShortFileSize(context, inv.archivedBytes ?: 0L)), 13f, palette.ink) }
            inv.importedFitFiles?.let { lines += label(context.getString(R.string.sd_cap_fit, it), 13f, palette.ink) }
            inv.regularObservations?.let { lines += label(context.getString(R.string.sd_cap_obs, it), 13f, palette.ink) }
            inv.candidatesByMetric?.takeIf { it.isNotEmpty() }?.let { m ->
                lines += label(context.getString(R.string.sd_cap_cand, m.entries.joinToString(", ") { "${candidateName(it.key)} ${it.value}" }), 13f, palette.ink)
            }
            inv.rawNotifications?.let { lines += label(context.getString(R.string.sd_cap_raw, it), 13f, palette.ink) }
            inv.rawFitFields?.let { lines += label(context.getString(R.string.sd_cap_fields, it), 13f, palette.ink) }
            inv.undecodedFieldKinds?.takeIf { it > 0 }?.let { lines += label(context.getString(R.string.sd_cap_undecoded, it), 13f, palette.ink) }
            if (inv.hasUnreadable) lines += label(context.getString(R.string.sd_cap_partial), 12f, palette.muted)
            lines += label(context.getString(R.string.hsp_coverage_note), 12f, palette.muted)
        }
        return card(context.getString(R.string.sd_cap_title), *lines.toTypedArray()).apply { tag = "science_capture" }
    }

    /** Over 7/30 days a mostly empty chart is hard to read: say honestly on how many days values exist at all. */
    private fun sparseDaysHint(data: SeriesData): String? {
        val rangeDays = (ranges[selected].second / 24).toInt()
        if (rangeDays < 7 || data.points.isEmpty()) return null
        val zone = java.time.ZoneId.systemDefault()
        val days = data.points.mapNotNull { p -> p.timeMs?.let { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } }.toSet().size
        return if (days * 2 < rangeDays) context.getString(R.string.sd_days_hint, days, rangeDays) else null
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
