package de.pulselibre

// BMI lives in ProfileMath.Bmi (single implementation, adult rating from 20 years, see docs/science/BMI_CLASSIFICATION.md).

/** WHO 2020: adult weekly moderate-intensity aerobic activity lower bound. */
fun whoModerateMinutesPerWeek(ageYears: Int): Int? = if (ageYears >= 18) 150 else null

/** WHO 2020: adult weekly vigorous-intensity aerobic activity lower bound. */
fun whoVigorousMinutesPerWeek(ageYears: Int): Int? = if (ageYears >= 18) 75 else null
