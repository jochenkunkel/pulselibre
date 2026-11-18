package de.pulselibre

import de.pulselibre.Bmi.Category
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AgeCalculatorTest {
    private val birth = LocalDate.of(1983, 6, 15)
    @Test fun ageIsOneLowerBeforeTheBirthdayInTheCurrentYear() {
        assertEquals(42, AgeCalculator.ageOn(birth, LocalDate.of(2026, 6, 14)))
        assertEquals(43, AgeCalculator.ageOn(birth, LocalDate.of(2026, 6, 15)))
    }
    @Test fun birthdayAndNewYearBoundaries() {
        assertEquals(42, AgeCalculator.ageOn(LocalDate.of(1983, 12, 31), LocalDate.of(2026, 12, 30)))
        assertEquals(43, AgeCalculator.ageOn(LocalDate.of(1983, 12, 31), LocalDate.of(2026, 12, 31)))
        assertEquals(43, AgeCalculator.ageOn(LocalDate.of(1983, 1, 1), LocalDate.of(2026, 12, 31)))
        assertEquals(0, AgeCalculator.ageOn(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
    }
    @Test fun leapDayBirthdayCountsFromFirstMarchInNonLeapYears() {
        val leap = LocalDate.of(2000, 2, 29)
        assertEquals(24, AgeCalculator.ageOn(leap, LocalDate.of(2025, 2, 28)))
        assertEquals(25, AgeCalculator.ageOn(leap, LocalDate.of(2025, 3, 1)))
        assertEquals(24, AgeCalculator.ageOn(leap, LocalDate.of(2024, 2, 29)))
    }
    @Test fun futureAndImplausibleBirthDatesGiveNoAge() {
        assertNull(AgeCalculator.ageOn(LocalDate.of(2030, 1, 1), LocalDate.of(2026, 11, 18)))
        assertNull(AgeCalculator.ageOn(LocalDate.of(1800, 1, 1), LocalDate.of(2026, 11, 18)))
    }
    @Test fun parseRejectsGarbageAndBlank() {
        assertNull(AgeCalculator.parse(null)); assertNull(AgeCalculator.parse("")); assertNull(AgeCalculator.parse("31.12.1983"))
        assertNull(AgeCalculator.parse("1983-02-30")); assertEquals(birth, AgeCalculator.parse(" 1983-06-15 "))
    }
    @Test fun legacyAgeIsUsedOnlyWithoutBirthDateAndNeverTurnsIntoABirthDate() {
        val today = LocalDate.of(2026, 11, 18)
        assertEquals(43, AgeCalculator.currentAge("", "43", today))
        assertEquals(43, AgeCalculator.currentAge("1983-06-15", "99", today)) // birth date wins
        assertNull(AgeCalculator.currentAge("", "0", today)); assertNull(AgeCalculator.currentAge("", "abc", today))
        assertNull(AgeCalculator.currentAge(null, null, today))
        assertNull(AgeCalculator.currentAge("2030-01-01", "43", today)) // invalid birth date is not silently replaced
    }
}

class BmiTest {
    @Test fun formulaMatchesIssueExample() {
        assertEquals(24.69, Bmi.compute(80.0, 180.0)!!, 0.01)
    }
    @Test fun boundariesUseUnroundedValue() {
        assertEquals(Category.UNDERWEIGHT, Bmi.classifyAdult(18.4999))
        assertEquals(Category.NORMAL, Bmi.classifyAdult(18.5))
        assertEquals(Category.NORMAL, Bmi.classifyAdult(24.9999))
        assertEquals(Category.PRE_OBESITY, Bmi.classifyAdult(25.0))
        assertEquals(Category.PRE_OBESITY, Bmi.classifyAdult(29.9999))
        assertEquals(Category.OBESITY_1, Bmi.classifyAdult(30.0))
        assertEquals(Category.OBESITY_1, Bmi.classifyAdult(34.9999))
        assertEquals(Category.OBESITY_2, Bmi.classifyAdult(35.0))
        assertEquals(Category.OBESITY_2, Bmi.classifyAdult(39.9999))
        assertEquals(Category.OBESITY_3, Bmi.classifyAdult(40.0))
    }
    @Test fun roundedDisplayValueDoesNotChangeTheCategory() {
        // 24.96 would display as 25,0 but is still Normalgewicht
        val w = 24.96 * 1.8 * 1.8
        assertEquals(Category.NORMAL, Bmi.assess(w, 180.0, 40).category)
    }
    @Test fun tonesFollowTheIssue() {
        assertEquals(Bmi.Tone.YELLOW, Category.UNDERWEIGHT.tone); assertEquals(Bmi.Tone.GREEN, Category.NORMAL.tone)
        assertEquals(Bmi.Tone.YELLOW, Category.PRE_OBESITY.tone)
        listOf(Category.OBESITY_1, Category.OBESITY_2, Category.OBESITY_3).forEach { assertEquals(Bmi.Tone.RED, it.tone) }
    }
    @Test fun missingOrInvalidInputGivesPlaceholderNotAValue() {
        listOf(null to 180.0, 80.0 to null, 0.0 to 180.0, -5.0 to 180.0, 80.0 to 0.0, 80.0 to 20.0, Double.NaN to 180.0, 500.0 to 180.0)
            .forEach { (w, h) -> val a = Bmi.assess(w, h, 40); assertNull("$w/$h", a.value); assertNull(a.category); assertNotNull(a.note) }
    }
    @Test fun youngerThanTwentyGetsNoAdultRating() {
        val a = Bmi.assess(60.0, 170.0, 19)
        assertNotNull(a.value); assertNull(a.category); assertTrue(a.note!!.contains("Altersgruppe"))
        assertNotNull(Bmi.assess(60.0, 170.0, 20).category)
    }
    @Test fun unknownAgeShowsValueButAsksForBirthDate() {
        val a = Bmi.assess(80.0, 180.0, null)
        assertNotNull(a.value); assertNull(a.category); assertTrue(a.note!!.contains("Geburtsdatum"))
    }
}
