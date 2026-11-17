package de.pulselibre

/**
 * Offline coordinator: 5002 success -> 5004 chunks -> validated directory or file bytes.
 * No transport, outbound GFDI requests, ACKs or persistence.
 * CRC verification is injected: DEX confirms Connect's final accumulator
 * comparison, but neither that behavior nor the command is CIRQA-verified.
 */
class CirqaLegacyOfflineFileSession(
    private val fileIndex: Int,
    private val verifyPayloadCrc: (ByteArray, Int) -> Boolean,
    private val maxFileBytes: Int = 16 * 1024 * 1024
) {
    enum class Phase { AWAITING_READ_RESPONSE, RECEIVING, COMPLETE, FAILED }
    var phase: Phase = Phase.AWAITING_READ_RESPONSE
        private set
    var readStatus: CirqaLegacyTransferDecoder.ReadStatus? = null
        private set
    private var assembler: CirqaLegacyPlainFileAssembler? = null

    init {
        require(fileIndex in 0..65535)
        require(maxFileBytes > 0)
    }

    /**
     * Starts from the now-reconstructed Connect 5002 unsigned LE length.
     * Rejects zero, overflow and sizes beyond the configured maximum.
     * Offline only; does not assert CIRQA compatibility.
     */
    fun beginFrom5002Response(readResponseBody: ByteArray): Boolean {
        if (phase != Phase.AWAITING_READ_RESPONSE) return false
        val verifiedSize = CirqaLegacy5002Codec.boundedSuccessSize(readResponseBody, maxFileBytes)
        if (verifiedSize == null) {
            readStatus = CirqaLegacy5002Codec.response(readResponseBody)?.status
            phase = Phase.FAILED
            return false
        }
        return beginWithVerifiedSize(readResponseBody, verifiedSize)
    }

    /**
     * Alternate entry point for callers with an independently validated size.
     * Prefer beginFrom5002Response for the reconstructed u32LE 5002 field.
     * This method does not compare verifiedSize with the response bytes.
     */
    fun beginWithVerifiedSize(readResponseBody: ByteArray, verifiedSize: Int): Boolean {
        if (phase != Phase.AWAITING_READ_RESPONSE) return false
        val response = CirqaLegacyTransferDecoder.readResponse(readResponseBody)
        readStatus = response?.status
        if (response?.status != CirqaLegacyTransferDecoder.ReadStatus.SUCCESS ||
            verifiedSize !in 1..maxFileBytes) {
            phase = Phase.FAILED
            return false
        }
        assembler = CirqaLegacyPlainFileAssembler(
            expectedSize = verifiedSize,
            verifyPayloadCrc = verifyPayloadCrc,
            maxFileBytes = maxFileBytes
        )
        phase = Phase.RECEIVING
        return true
    }

    fun accept5004(body: ByteArray): CirqaLegacyPlainFileAssembler.Result? {
        if (phase != Phase.RECEIVING) return null
        val result = assembler!!.accept(body)
        when (result) {
            CirqaLegacyPlainFileAssembler.Result.COMPLETE -> phase = Phase.COMPLETE
            CirqaLegacyPlainFileAssembler.Result.ABORTED -> phase = Phase.FAILED
            else -> Unit
        }
        return result
    }

    fun completedFile(): ByteArray? =
        if (phase == Phase.COMPLETE) assembler?.completedBytes() else null

    /**
     * Exposes a complete FIT file only after both transport-chunk validation
     * (injected CRC verifier) and the independent FIT envelope/file CRC pass.
     * This is offline validation, not proof of CIRQA provenance or authenticity.
     */
    fun completedVerifiedFit(): ByteArray? {
        if (fileIndex == 0) return null
        val candidate = completedFile() ?: return null
        return candidate.takeIf { FitFileHeader.inspectVerified(it) != null }
    }

    fun completedDirectory(): CirqaLegacyDirectoryParser.Directory? =
        if (fileIndex == 0) completedFile()?.let(CirqaLegacyDirectoryParser::parse) else null
}
