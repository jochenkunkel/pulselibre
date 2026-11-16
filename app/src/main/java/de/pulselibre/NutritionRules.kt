package de.pulselibre

/** DGE protein reference for healthy, normal-weight adults; not a clinical prescription. */
fun dgeProteinGramsPerDay(ageYears: Int, weightKg: Double, normalWeight: Boolean): Double? {
    if (ageYears < 19 || !normalWeight || !weightKg.isFinite() || weightKg <= 0.0) return null
    return weightKg * if (ageYears >= 65) 1.0 else 0.8
}

/** Converts an explicitly supplied energy estimate into kilojoules (exact unit conversion). */
fun kilocaloriesToKilojoules(kcal: Double): Double? =
    if (kcal.isFinite() && kcal >= 0.0) kcal * 4.184 else null

/** No automatic calorie estimate from heart rate alone: insufficient input and validation. */
fun heartRateOnlyEnergyEstimate(bpm: Int): Double? = null
