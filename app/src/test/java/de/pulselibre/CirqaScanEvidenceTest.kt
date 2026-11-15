package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaScanEvidenceTest {
    @Test fun unknownDevicesNeverCountAsCirqa() {
        val evidence = CirqaScanEvidence()
        evidence.observe("a", false, false)
        assertTrue(evidence.summary().contains("Bestätigte CIRQA-Werbeadressen: 0"))
        assertTrue(evidence.summary().contains("Nicht zuordenbare Werbegeräte: 1"))
    }

    @Test fun garminHintIsNotConfirmation() {
        val evidence = CirqaScanEvidence()
        evidence.observe("a", false, true)
        assertTrue(evidence.summary().contains("Garmin-Hinweise (nicht CIRQA-bestätigt): 1"))
        assertTrue(evidence.summary().contains("Bestätigte CIRQA-Werbeadressen: 0"))
    }

    @Test fun duplicateUpdatesDoNotInflateCounts() {
        val evidence = CirqaScanEvidence()
        evidence.observe("a", false, true)
        evidence.observe("a", false, true)
        evidence.observe("a", true, true)
        assertTrue(evidence.summary().contains("Bestätigte CIRQA-Werbeadressen: 1"))
        assertTrue(evidence.summary().contains("Garmin-Hinweise (nicht CIRQA-bestätigt): 0"))
    }

    @Test fun resetClearsPreviousScan() {
        val evidence = CirqaScanEvidence()
        evidence.observe("a", true, false)
        evidence.reset()
        assertTrue(evidence.summary().contains("Bestätigte CIRQA-Werbeadressen: 0"))
    }
}
