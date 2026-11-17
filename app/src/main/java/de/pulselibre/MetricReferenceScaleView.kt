package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/**
 * Source-backed context for displayed measurements; intervals are not diagnostic limits.
 * References: American Heart Association (reviewed 2024), “Target Heart Rates Chart”;
 * Oxford University Hospitals NHS Foundation Trust Respiratory Medicine (2022),
 * “How to use your pulse oximeter”, OMI 82415; MedlinePlus / Brodkey (reviewed 2025),
 * “Rapid shallow breathing”; World Health Organization (2020), “WHO guidelines on
 * physical activity and sedentary behaviour”. Stable source URLs are carried by each profile.
 */
data class MetricReferenceProfile(
    val minimum: Int,
    val maximum: Int,
    val unit: String,
    val referenceMinimum: Int? = null,
    val referenceMaximum: Int? = null,
    val referenceLabel: String? = null,
    val sourceName: String? = null,
    val sourceUrl: String? = null,
    val caveat: String
) {
    companion object {
        fun forMetric(metric: DashboardMetric, stepGoal: Int? = null): MetricReferenceProfile? = when (metric.title) {
            "Herzfrequenz" -> MetricReferenceProfile(40, 180, "bpm", 60, 100,
                "Erwachsene in Ruhe", "American Heart Association",
                "https://www.heart.org/en/healthy-living/exercise-and-physical-activity/fitness-basics/target-heart-rates",
                "Der aktuelle Puls ist aktivitätsabhängig; die Ruhe-Referenz ist hier nur Kontext.")
            "Ruhepuls" -> MetricReferenceProfile(0, 180, "bpm", 60, 100,
                "Erwachsene in Ruhe", "American Heart Association",
                "https://www.heart.org/en/healthy-living/exercise-and-physical-activity/fitness-basics/target-heart-rates",
                "Allgemeine Orientierung, kein persönlicher Ziel- oder Diagnosewert.")
            "Sauerstoffsättigung · SpO₂" -> MetricReferenceProfile(0, 100, "%", 95, 100,
                "Durchschnittliche Erwachsene, in Ruhe", "Oxford University Hospitals NHS",
                "https://www.ouh.nhs.uk/redirecthandler/?id=82415Poximeter&type=patientleaflet",
                "Finger-Pulsoximeter-Orientierung; nicht für CIRQA am Handgelenk validiert.")
            "Atemfrequenz" -> MetricReferenceProfile(0, 40, "/min", 12, 20,
                "Erwachsene in Ruhe", "MedlinePlus",
                "https://medlineplus.gov/ency/article/007198.htm",
                "Gilt für Erwachsene in Ruhe; Momentanwert kann durch Aktivität abweichen.")
            "Stress (Geräteskala)", "Body Battery (Geräteskala)" -> MetricReferenceProfile(
                0, 100, "/100", caveat = "Proprietäre Geräteskala; kein klinischer Normbereich.")
            "RR-Intervall (kein RMSSD)" -> MetricReferenceProfile(0, 2000, "ms",
                caveat = "Einzelnes RR-Intervall; keine HRV-Kennzahl und kein Normvergleich.")
            "Schritte (Tageszähler)" -> stepGoal?.takeIf { it > 0 }?.let {
                MetricReferenceProfile(0, it, "Schritte", caveat = "Fortschritt zum geräteeigenen Tagesziel, keine WHO-Norm.")
            }
            "Tagesziel Schritte" -> MetricReferenceProfile(
                0, 100, "%", caveat = "Produktfortschritt zum vom Gerät gelieferten Tagesziel; keine Gesundheitsbewertung oder Bewegungsempfehlung."
            )
            else -> null
        }
    }
}

/** Small linear plot: measured value in the accent color, reference interval as a marker only. */
class MetricReferenceScaleView(
    context: Context,
    private val profile: MetricReferenceProfile,
    private val value: Int,
    private val accent: Int,
    private val ink: Int,
    private val muted: Int,
    private val track: Int,
    private val referenceColor: Int
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bar = RectF()
    private val density = resources.displayMetrics.density

    init {
        contentDescription = buildString {
            append("Skala ${profile.minimum} bis ${profile.maximum} ${profile.unit}; Wert $value ${profile.unit}. ")
            profile.referenceLabel?.let { append("Orientierungsbereich ${profile.referenceMinimum} bis ${profile.referenceMaximum}, $it. ") }
            append(profile.caveat)
        }
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = 2f * density
        val right = width - 2f * density
        if (right <= left) return
        val top = 4f * density
        val bottom = top + 5f * density
        bar.set(left, top, right, bottom)
        paint.style = Paint.Style.FILL
        paint.color = track
        canvas.drawRoundRect(bar, 3f * density, 3f * density, paint)
        val span = (profile.maximum - profile.minimum).coerceAtLeast(1)
        val refMin = profile.referenceMinimum
        val refMax = profile.referenceMaximum
        var referenceBounds: Pair<Float, Float>? = null
        if (refMin != null && refMax != null) {
            val x1 = left + ((refMin - profile.minimum).toFloat() / span).coerceIn(0f, 1f) * (right - left)
            val x2 = left + ((refMax - profile.minimum).toFloat() / span).coerceIn(0f, 1f) * (right - left)
            referenceBounds = x1 to x2
            paint.color = referenceColor
            paint.alpha = 75
            canvas.drawRect(x1, top, x2, bottom, paint)
            paint.alpha = 255
        }
        val fraction = ((value - profile.minimum).toFloat() / span).coerceIn(0f, 1f)
        bar.right = left + fraction * (right - left)
        paint.color = accent
        canvas.drawRoundRect(bar, 3f * density, 3f * density, paint)
        referenceBounds?.let { (x1, x2) ->
            paint.color = referenceColor
            paint.strokeWidth = 1.5f * density
            paint.strokeCap = Paint.Cap.SQUARE
            canvas.drawLine(x1, top - density, x1, bottom + density, paint)
            canvas.drawLine(x2, top - density, x2, bottom + density, paint)
        }
        paint.color = muted
        paint.textSize = 9f * density
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("${profile.minimum}", left, height - 1f * density, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("${profile.maximum} ${profile.unit}", right, height - 1f * density, paint)
    }
}
