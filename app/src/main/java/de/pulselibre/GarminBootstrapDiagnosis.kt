package de.pulselibre

/**
 * Passive bootstrap diagnosis for CIRQA. Does not transmit the fenix 8
 * watch-emulation frames: they contain watch identity and are not validated
 * for CIRQA. Reports only frame types and counters, never payload bytes.
 */
class GarminBootstrapDiagnosis {
    private val counts = linkedMapOf<Int, Int>()
    var validFrames = 0
        private set
    var deviceInfoRepeats = 0
        private set
    var statusFrames = 0
        private set
    var otherFrames = 0
        private set
    private var seenDeviceInfo = false

    fun observe(frame: ByteArray) {
        if (GarminFrameValidator.inspectVerified(frame) == null || frame.size < 6) return
        val raw = u16(frame, 2)
        val type = if (raw and 0x8000 != 0) 5000 + (raw and 255) else raw
        validFrames++
        counts[type] = (counts[type] ?: 0) + 1
        when (type) {
            5024 -> {
                if (seenDeviceInfo) deviceInfoRepeats++
                seenDeviceInfo = true
            }
            5000 -> statusFrames++
            else -> otherFrames++
        }
    }

    fun summary(): String =
        "Bootstrap-Diagnose (passiv): gültig=$validFrames; DeviceInfo-Wiederholungen=$deviceInfoRepeats; " +
        "Status=$statusFrames; weitere=$otherFrames; Typen=$counts; " +
        "aktive Bootstrap-Befehle=0"

    private fun u16(bytes: ByteArray, i: Int) =
        (bytes[i].toInt() and 255) or ((bytes[i + 1].toInt() and 255) shl 8)
}
