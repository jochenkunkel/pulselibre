package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5050StructureObserverTest {
    private fun frame(body: ByteArray): ByteArray {
        val length = body.size + 6
        val prefix = byteArrayOf(length.toByte(), (length ushr 8).toByte(),
            5050.toByte(), (5050 ushr 8).toByte()) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    @Test fun tracksRetriesAndChangesWithoutExposingPayload() {
        val observer = Cirqa5050StructureObserver()
        observer.observe(frame(byteArrayOf(1, 2, 3)))
        observer.observe(frame(byteArrayOf(1, 2, 3)))
        observer.observe(frame(byteArrayOf(1, 2, 4)))
        val summary = observer.summary()
        assertTrue(summary.contains("Frames=3"))
        assertTrue(summary.contains("identische Wiederholungen=1"))
        assertTrue(summary.contains("Payloadwechsel=1"))
        assertTrue(summary.contains("gemeinsame Präfixlängen bei Wechsel={2=1}"))
        assertTrue(summary.contains("automatische Antwort=NEIN"))
    }

    @Test fun characterizesBitmapAndTailWithoutPublishingBytes() {
        val observer = Cirqa5050StructureObserver()
        observer.observe(frame(byteArrayOf(2, 8, 16, 99)))
        observer.observe(frame(byteArrayOf(3, 1)))
        observer.observe(frame(byteArrayOf(0)))
        val report = observer.summary()
        assertTrue(report.contains("5050-Längenpräfix gültig=2/ungültig=1"))
        assertTrue(report.contains("Bitmap-Längen={2=1, 0=1}"))
        assertTrue(report.contains("Tail-Längen={1=1, 0=1}"))
    }

    @Test fun ignoresInvalidCrcAndOtherTypes() {
        val observer = Cirqa5050StructureObserver()
        val invalid = frame(byteArrayOf(5))
        invalid[invalid.lastIndex] = (invalid.last().toInt() xor 1).toByte()
        observer.observe(invalid)
        observer.observe(GarminDeviceInformationAckCandidate.frame())
        assertTrue(observer.summary().contains("Frames=0"))
    }
}
