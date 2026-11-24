package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

/** The display mapping must be the identity in German: every domain phrase keeps its wording, only other languages change. */
class DomainTextTest {
    private fun same(vararg phrases: String) = phrases.forEach { assertEquals(it, DomainText.translate(GermanStrings, it)) }

    @Test fun everyFixedPhraseIsUnchangedInGerman() {
        // "Keine Messdaten" and similar are mapped to resource keys; the German resource must carry the identical text.
        DomainText.knownPhrases().forEach { assertEquals(it, DomainText.translate(GermanStrings, it)) }
    }

    @Test fun sentencePatternsKeepTheirParameters() = same(
        "Zu wenige Tage mit Daten (3 von mindestens 4).",
        "Vergleichszeitraum hat zu wenige Daten (2 von mindestens 14).",
        "Median der letzten 28 Tage gegen die 28 Tage davor.",
        "Nur 12 gepaarte Tage, mindestens 30 nötig.",
        "Spearman ρ, 95-%-Intervall mit effektivem n≈23; keine Aussage über Ursache und Wirkung.",
        "Robustheit: ohne einzelne Tage ρ 0,1 bis 0,3; ohne die 2 auffälligsten Tage ρ 0,2 – stabil.",
        "Robustheit: ohne einzelne Tage ρ 0,1 bis 0,3; ohne die 2 auffälligsten Tage ρ 0,2 – nicht stabil (einzelne Tage beeinflussen das Ergebnis).",
        "+1,2 bpm gegenüber vorherigen 28 Tagen",
        "Vom Gerät berechneter Ruhepuls; Vergleich mit den 28 Tagen davor. Keine Fitnessdiagnose.",
        "Ruhepuls: 12 von 28 Tagen",
        "für eine Trendaussage fehlen Tage (mindestens 14 nötig)",
        "5 von 7 Tagen",
        "Ziel (8000) erreicht an 3 von 5 Tagen mit Daten.",
        "12.345 (nur Tage mit Daten)",
        "12 von 28 Tagen mit verifizierten Schrittwerten",
        "Nur ein Datenkanal verfügbar (Ruhepuls); keine Aussage zur Fitness.",
        "Eigenes Ziel (8.000) erreicht an 3 von 5 Tagen mit Daten (28-Tage-Fenster)",
        "Quelle: CIRQA-Service 12",
        "Zeitfenster kürzer als 6 h.",
        "Abdeckung 12,5 % unter 50 %.",
        "Abtastabstand zu groß für ODI (Median 120 s, nötig ≤ 60 s).",
    )

    @Test fun germanNumbersAreShownWithTheSeparatorsOfTheLanguage() {
        val en = java.util.Locale.ENGLISH
        val fr = java.util.Locale.FRENCH
        assertEquals("1.5", DomainText.localizeNumber("1,5", en))
        assertEquals("+2.5 bpm", DomainText.localizeNumber("+2,5 bpm", en))
        assertEquals("12,345", DomainText.localizeNumber("12.345", en))
        assertEquals("1,234,567.8", DomainText.localizeNumber("1.234.567,8", en))
        assertEquals("1,5", DomainText.localizeNumber("1,5", fr))
        assertEquals("70.3 bpm", DomainText.localizeNumber("70,3 bpm", en))
        // not a German number: dates, ranges, times and words stay untouched
        listOf("19.11.", "19.11. 00:00", "60–<70", "28", "heute", "1.5", "12.34").forEach { assertEquals(it, DomainText.localizeNumber(it, en)) }
    }

    @Test fun germanOutputIsUnchangedByNumberLocalization() = same(
        "+1,2 bpm gegenüber vorherigen 28 Tagen", "Ziel (8.000) erreicht an 3 von 5 Tagen mit Daten.")
}
