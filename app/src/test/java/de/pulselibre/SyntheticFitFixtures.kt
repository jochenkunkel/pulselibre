package de.pulselibre

import java.security.MessageDigest

/**
 * Deterministic, fully synthetic FIT fixtures for offline regression tests.
 *
 * Provenance: `generated-fixture`. Every byte is constructed here from literal
 * placeholder values; no byte originates from a device, BLE/GATT trace,
 * pairing, original export, or health record. The placeholder heart rate
 * (42 bpm) is a fixed test constant, not a measurement.
 *
 * Validation: the 14-byte FIT envelope and both CRCs are computed with the
 * production [FitFileHeader.crc16]; [FitFileHeader.inspectVerified] and
 * [FitHistoricalObservations.decode] expectations are asserted in
 * [SyntheticFitFixturesTest]; every published byte sequence is pinned by an
 * explicit expected payload and SHA-256 below.
 *
 * Publication basis: `docs/collaboration/CURRENT_BAND_RAW_DATA_POLICY.md`
 * (Ergänzung 22.11.2026) and the AGENTS.md project decision on vetted
 * synthetic research fixtures. These fixtures are not evidence of complete
 * device history or of production import of any CIRQA measurement type.
 */
object SyntheticFitFixtures {
    const val ORIGIN = "generated-fixture"
    const val SYNTHETIC = true
    const val PLACEHOLDER_HEART_RATE_BPM = 42
    const val PROFILE_VERSION = 100
    const val PROTOCOL_VERSION = 0x20
    const val PROVENANCE_NOTE =
        "generated-fixture; synthetic placeholder bytes only; no device, trace, pairing or health data"
    const val VALIDATION_NOTE =
        "envelope and CRCs generated with production FitFileHeader.crc16; " +
            "inspectVerified, decoder result, payload and SHA-256 pinned in SyntheticFitFixturesTest"

    data class Fixture(
        val id: String,
        val purpose: String,
        val bytes: ByteArray,
        val expectedSha256: String,
        val expectedDataSize: Int,
        val expectedPayload: List<Int>,
        /** null means the decoder must reject this fixture outright. */
        val expectedDecodedObservations: Int?,
        val expectedVerified: Boolean
    )

    /** Literal synthetic record definition plus one placeholder heart-rate data record. */
    private val heartRatePayload = listOf(
        0x40, 0x00, 0x00, 20, 0x00, 0x02, // definition: global record 20, two fields
        0xfd, 0x04, 0x86, // field 253: uint32 timestamp placeholder
        0x03, 0x01, 0x02, // field 3: uint8 heart-rate placeholder
        0x00, // first data-record header
        0x10, 0x27, 0x00, 0x10, PLACEHOLDER_HEART_RATE_BPM
    )

    /** One importable placeholder observation; valid header CRC and file CRC. */
    val heartRatePlaceholder = Fixture(
        id = "hr-placeholder-v2-absolute-time",
        purpose = "positive import/CRC/decoder regression with one synthetic placeholder record",
        bytes = build(heartRatePayload),
        expectedSha256 = "9c6f9a7c2ecfa337cf99faf224df82c117300d06ece28f79af0cc63c1c813c8e",
        expectedDataSize = heartRatePayload.size,
        expectedPayload = heartRatePayload,
        expectedDecodedObservations = 1,
        expectedVerified = true
    )

    /** Valid empty envelope; proves an empty fixture never produces an observation. */
    val emptyPayload = Fixture(
        id = "empty-payload-v1",
        purpose = "valid envelope without records; zero decoded observations",
        bytes = build(emptyList()),
        expectedSha256 = "bb2a825cc4c918350deb04cc46d35dd482055a755b731012805c58c2ae02ddfc",
        expectedDataSize = 0,
        expectedPayload = emptyList(),
        expectedDecodedObservations = 0,
        expectedVerified = true
    )

    /** Same structure as [heartRatePlaceholder] with one flipped file-CRC byte. */
    val fileCrcTampered: Fixture = run {
        val tampered = build(heartRatePayload)
        tampered[tampered.lastIndex] = (tampered[tampered.lastIndex].toInt() xor 1).toByte()
        Fixture(
            id = "file-crc-tampered-v2",
            purpose = "negative fixture: structurally valid envelope, corrupted file CRC",
            bytes = tampered,
            expectedSha256 = "05f7933b4b2c3c52f030ac7bdd4a3d85ec6989d10a9dd1ae0acfea82c558ea4b",
            expectedDataSize = heartRatePayload.size,
            expectedPayload = heartRatePayload,
            expectedDecodedObservations = null,
            expectedVerified = false
        )
    }

    val all: List<Fixture> = listOf(heartRatePlaceholder, emptyPayload, fileCrcTampered)

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    /** Builds a 14-byte-header FIT file; CRCs are recomputed, never copied from a device. */
    private fun build(payload: List<Int>): ByteArray {
        val file = ByteArray(14 + payload.size + 2)
        file[0] = 14
        file[1] = PROTOCOL_VERSION.toByte()
        file[2] = PROFILE_VERSION.toByte()
        file[3] = (PROFILE_VERSION ushr 8).toByte()
        file[4] = payload.size.toByte()
        file[5] = (payload.size ushr 8).toByte()
        file[6] = (payload.size ushr 16).toByte()
        file[7] = (payload.size ushr 24).toByte()
        ".FIT".toByteArray(Charsets.US_ASCII).copyInto(file, 8)
        val headerCrc = FitFileHeader.crc16(file, 0, 12)
        file[12] = headerCrc.toByte()
        file[13] = (headerCrc ushr 8).toByte()
        payload.forEachIndexed { index, value -> file[14 + index] = value.toByte() }
        val fileCrc = FitFileHeader.crc16(file, 0, file.size - 2)
        file[file.size - 2] = fileCrc.toByte()
        file[file.size - 1] = (fileCrc ushr 8).toByte()
        return file
    }
}
