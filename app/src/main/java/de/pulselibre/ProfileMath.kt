package de.pulselibre

import java.time.LocalDate
import java.time.Period

/**
 * Age from a stored birth date (#117). The age is always derived for the reference day, never stored.
 * On a 29 February birthday the person counts as one year older on 1 March in non-leap years
 * (java.time Period semantics).
 */
internal object AgeCalculator {
    private const val MAX_PLAUSIBLE_AGE = 130

    fun parse(iso: String?): LocalDate? =
        iso?.trim()?.takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** Completed years on [today]; null for a birth date in the future or an implausibly old one. */
    fun ageOn(birth: LocalDate, today: LocalDate): Int? {
        if (birth.isAfter(today)) return null
        val years = Period.between(birth, today).years
        return years.takeIf { it <= MAX_PLAUSIBLE_AGE }
    }

    /**
     * Age for display/evaluation: derived from the birth date when present, otherwise the legacy stored age
     * (an exact birth date is never invented from it). [legacyAge] outside 1..120 is ignored.
     */
    fun currentAge(birthDateIso: String?, legacyAge: String?, today: LocalDate): Int? {
        parse(birthDateIso)?.let { return ageOn(it, today) }
        return legacyAge?.trim()?.toIntOrNull()?.takeIf { it in 1..120 }
    }
}

/**
 * Body-mass index and its WHO classification for adults (#118). The BMI is a screening indicator, not a diagnosis.
 * Reference: WHO, "Obesity: preventing and managing the global epidemic" (WHO TRS 894, 2000) and WHO Europe BMI
 * classification: <18.5 underweight, 18.5–24.99 normal, 25–29.99 pre-obesity, 30–34.99 / 35–39.99 / ≥40 obesity I/II/III.
 * Thresholds are applied to the unrounded value and are identical for men and women.
 */
internal object Bmi {
    enum class Tone { GREEN, YELLOW, RED }
    enum class Category(val label: String, val tone: Tone) {
        UNDERWEIGHT("Untergewicht", Tone.YELLOW),
        NORMAL("Normalgewicht", Tone.GREEN),
        PRE_OBESITY("Präadipositas", Tone.YELLOW),
        OBESITY_1("Adipositas Grad I", Tone.RED),
        OBESITY_2("Adipositas Grad II", Tone.RED),
        OBESITY_3("Adipositas Grad III", Tone.RED)
    }

    /** Adult classification starts at this age; younger people need BMI-for-age references (not implemented). */
    const val ADULT_FROM_AGE = 20

    data class Assessment(val value: Double?, val category: Category?, val note: String?)

    /** kg / m²; null for missing or implausible input (weight 15–400 kg, height 50–250 cm). */
    fun compute(weightKg: Double?, heightCm: Double?): Double? {
        if (weightKg == null || heightCm == null || !weightKg.isFinite() || !heightCm.isFinite()) return null
        if (weightKg !in 15.0..400.0 || heightCm !in 50.0..250.0) return null
        val m = heightCm / 100.0
        return weightKg / (m * m)
    }

    fun classifyAdult(bmi: Double): Category = when {
        bmi < 18.5 -> Category.UNDERWEIGHT
        bmi < 25.0 -> Category.NORMAL
        bmi < 30.0 -> Category.PRE_OBESITY
        bmi < 35.0 -> Category.OBESITY_1
        bmi < 40.0 -> Category.OBESITY_2
        else -> Category.OBESITY_3
    }

    fun assess(weightKg: Double?, heightCm: Double?, ageYears: Int?): Assessment {
        val bmi = compute(weightKg, heightCm)
            ?: return Assessment(null, null, "Größe und Gewicht eintragen, um den BMI zu sehen.")
        return when {
            ageYears == null -> Assessment(bmi, null, "Geburtsdatum ergänzen, um den BMI einzuordnen.")
            ageYears < ADULT_FROM_AGE -> Assessment(bmi, null, "Für diese Altersgruppe keine BMI-Bewertung verfügbar.")
            else -> Assessment(bmi, classifyAdult(bmi), null)
        }
    }
}

/** Weight stepping for the quick-weight dialog: ±0.1 kg, one decimal, clamped to the plausible range. */
internal object WeightStepper {
    const val MIN_KG = 15.0
    const val MAX_KG = 400.0
    const val STEP_KG = 0.1
    /** Only a starting point inside the editor when nothing is stored; it is saved solely if the user confirms. */
    const val START_KG = 70.0

    fun parse(text: String?): Double? = text?.trim()?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it.isFinite() }

    fun step(text: String?, deltaKg: Double): Double {
        val base = parse(text)?.coerceIn(MIN_KG, MAX_KG) ?: START_KG
        val raw = Math.round((base + deltaKg) * 10.0) / 10.0
        return raw.coerceIn(MIN_KG, MAX_KG)
    }

    /** "80" for whole numbers is shown as "80,0" so the decimal step is visible; decimal separator of [locale] (default German). */
    fun format(kg: Double, locale: java.util.Locale = java.util.Locale.GERMANY): String = String.format(locale, "%.1f", kg)
}
