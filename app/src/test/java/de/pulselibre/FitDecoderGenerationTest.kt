package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FitDecoderGenerationTest {
    private fun row(sampleId: String, value: Double = 72.0) = StoredFitGenerationObservation(
        generationId = "g".repeat(64),
        sampleId = sampleId,
        deviceId = "cirqa",
        metric = "heart_rate",
        measuredAtEpochMillis = 123_456,
        value = value,
        unit = "bpm",
        receivedAtEpochMillis = null,
        recordOrdinal = 4,
        globalMessageNumber = 20,
        fieldNumber = 3,
        quality = "PROFILE_VALID",
        timestampOrigin = "DIRECT"
    )

    @Test fun identityIsStableAndSeparatesSourceDecoderAndProfile() {
        val source = "a".repeat(64)
        val id = FitDecoderGeneration.id(source, "decoder-1", "profile-1")

        assertEquals(64, id.length)
        assertEquals(id, FitDecoderGeneration.id(source, "decoder-1", "profile-1"))
        assertNotEquals(id, FitDecoderGeneration.id("b".repeat(64), "decoder-1", "profile-1"))
        assertNotEquals(id, FitDecoderGeneration.id(source, "decoder-2", "profile-1"))
        assertNotEquals(id, FitDecoderGeneration.id(source, "decoder-1", "profile-2"))
    }

    @Test fun outputDigestIsIndependentOfRowOrderButDetectsChangedOutput() {
        val first = row("sample-a")
        val second = row("sample-b", value = 75.0)
        val digest = FitDecoderGeneration.outputDigest(listOf(first, second))
        val laterReceipt = first.copy(receivedAtEpochMillis = 999_999)

        assertEquals(digest, FitDecoderGeneration.outputDigest(listOf(second, first)))
        assertEquals(digest, FitDecoderGeneration.outputDigest(listOf(laterReceipt, second)))
        assertNotEquals(digest, FitDecoderGeneration.outputDigest(listOf(first, row("sample-b", 76.0))))
        assertNotEquals(digest, FitDecoderGeneration.outputDigest(listOf(first)))
    }
}
