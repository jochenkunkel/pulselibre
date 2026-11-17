package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5050PostWriteEvidenceTest {
    @Test fun excludesPreWriteFrames() {
        val observer = Cirqa5050PostWriteEvidence()
        observer.observe(mapOf("gfdi-5050" to 1))
        assertTrue(observer.summary().contains("Beobachtung aktiv=false"))
        observer.onWriteConfirmed(mapOf("gfdi-5050" to 1, "gfdi-5000" to 1))
        observer.observe(mapOf("gfdi-5050" to 1, "gfdi-5000" to 1))
        assertTrue(observer.summary().contains("nachfolgende GFDI-Frames=0"))
        observer.observe(mapOf("gfdi-5050" to 2, "gfdi-5000" to 2, "gfdi-5011" to 1))
        assertTrue(observer.summary().contains("nachfolgende GFDI-Frames=3"))
        assertTrue(observer.summary().contains("5011-Frames=1"))
    }

    @Test fun doesNotTreatStatusAsHistoryUnlock() {
        val observer = Cirqa5050PostWriteEvidence()
        observer.onWriteConfirmed(emptyMap())
        observer.observe(mapOf("gfdi-5000" to 1))
        assertTrue(observer.summary().contains("Historienfreigabe=nicht nachgewiesen"))
    }
}
