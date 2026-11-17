package de.pulselibre

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.Instant

/** Small, testable model for the user-facing laboratory results and safe report. */
object CirqaTestLaboratory {
    enum class TestId(val label: String) {
        ANDROID_STATUS("Android-Bluetoothstatus"),
        PASSIVE_SCAN("Passiver BLE-Scan"),
        PASSIVE_PROTOCOL("Passive CIRQA-Protokollevidenz"),
        GATT_DISCOVERY("GATT-Dienstsuche"),
        LIVE_READINGS("Unterstützte Live-Messwerte"),
        FIT_FILE("Lokale FIT-Analyse"),
        HISTORICAL_DOWNLOAD("Historischer CIRQA-Download")
    }

    enum class Verdict(val label: String) {
        PASSED("Bestanden"),
        FAILED("Fehlgeschlagen"),
        NOT_TESTED("Nicht getestet"),
        NOT_SUPPORTED("Nicht unterstützt")
    }

    enum class EvidenceLevel(val label: String) {
        DEVICE_CONFIRMED("Am CIRQA-Gerät bestätigt"),
        PASSIVELY_OBSERVED("Passiv beobachtet"),
        OFFLINE_VALIDATED("Offline validiert"),
        UNPROVEN("Unbewiesen")
    }

    data class Record(
        val test: TestId,
        val verdict: Verdict,
        val evidence: EvidenceLevel,
        val checkedAtEpochMillis: Long?,
        /** Caller supplies only a count/status summary, never IDs, payloads, names or values. */
        val safeSummary: String
    )

    fun initialRecords(): List<Record> = TestId.values().map { test ->
        Record(
            test,
            Verdict.NOT_TESTED,
            if (test == TestId.HISTORICAL_DOWNLOAD || test == TestId.PASSIVE_PROTOCOL) EvidenceLevel.UNPROVEN
            else EvidenceLevel.UNPROVEN,
            null,
            when (test) {
                TestId.HISTORICAL_DOWNLOAD -> "Gesperrt; nicht freigegeben."
                TestId.PASSIVE_PROTOCOL -> "Keine passive CIRQA-Framequelle vorhanden; keine Anfrage gesendet."
                else -> "Noch kein Ergebnis."
            }
        )
    }

    fun report(records: List<Record>, generatedAtEpochMillis: Long): String = buildString {
        appendLine("PulsLibre · CIRQA-Testlabor")
        appendLine("Erstellt: ${Instant.ofEpochMilli(generatedAtEpochMillis)}")
        appendLine("Datenschutz: Bericht enthält keine Gerätekennung, Adresse, Rohpayloads, Dateinamen oder Messwerte.")
        appendLine("FIT-Dateien werden ausschließlich lokal geprüft und nicht in die Messwertdatenbank importiert.")
        appendLine("Historischer CIRQA-Download: NICHT FREIGEGEBEN; es wurden keine FileSync-Kommandos gesendet.")
        appendLine()
        TestId.values().forEach { test ->
            val record = records.firstOrNull { it.test == test }
                ?: initialRecords().first { it.test == test }
            appendLine("${test.label}: ${record.verdict.label} · ${record.evidence.label}")
            appendLine("  Zeit: ${record.checkedAtEpochMillis?.let(Instant::ofEpochMilli) ?: "–"}")
            appendLine("  Ergebnis: ${record.safeSummary}")
        }
    }
}

/** Reads SAF content with a hard cap; null means the stream exceeded the allowed size. */
object BoundedFitInput {
    fun read(input: InputStream, maxBytes: Int = FitFileHeader.MAX_FILE_BYTES): ByteArray? {
        require(maxBytes > 0)
        val output = ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) return output.toByteArray()
            if (read == 0) continue
            if (read > maxBytes - total) return null
            output.write(buffer, 0, read)
            total += read
            if (total == maxBytes) {
                if (input.read() != -1) return null
                return output.toByteArray()
            }
        }
    }
}
