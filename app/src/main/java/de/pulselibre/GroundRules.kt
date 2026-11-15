package de.pulselibre

/** WHO adult BMI formula. Null means unavailable or invalid; not a diagnosis. */
fun adultBmi(weightKg: Double, heightCm: Double, ageYears: Int): Double? {
    if (ageYears < 18 || !weightKg.isFinite() || !heightCm.isFinite() || weightKg <= 0.0 || heightCm <= 0.0) return null
    val meters = heightCm / 100.0
    return (weightKg / (meters * meters)).takeIf { it.isFinite() }
}

enum class AdultBmiCategory { UNDERWEIGHT, NORMAL_RANGE, OVERWEIGHT, OBESITY }

/** WHO adult population categories, not individual medical advice. */
fun adultBmiCategory(bmi: Double?): AdultBmiCategory? = when {
    bmi == null || !bmi.isFinite() || bmi <= 0.0 -> null
    bmi < 18.5 -> AdultBmiCategory.UNDERWEIGHT
    bmi < 25.0 -> AdultBmiCategory.NORMAL_RANGE
    bmi < 30.0 -> AdultBmiCategory.OVERWEIGHT
    else -> AdultBmiCategory.OBESITY
}

/** WHO 2020: adult weekly moderate-intensity aerobic activity lower bound. */
fun whoModerateMinutesPerWeek(ageYears: Int): Int? = if (ageYears >= 18) 150 else null

/** WHO 2020: adult weekly vigorous-intensity aerobic activity lower bound. */
fun whoVigorousMinutesPerWeek(ageYears: Int): Int? = if (ageYears >= 18) 75 else null
