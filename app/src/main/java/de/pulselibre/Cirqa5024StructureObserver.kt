package de.pulselibre

/**
 * Passive, metadata-only characterization of CRC-verified 5024 frames.
 * Never includes unit ID, strings, device identity or raw payload in reports.
 */
class Cirqa5024StructureObserver {
    var observed = 0
        private set
    var parsed = 0
        private set
    var rejected = 0
        private set
    private val bodyLengths = linkedMapOf<Int, Int>()
    private val optionalLengths = linkedMapOf<Int, Int>()

    fun observe(frame: ByteArray) {
        if (GarminFrameValidator.inspectVerified(frame) == null || frame.size < 6) return
        val type = (frame[2].toInt() and 255) or ((frame[3].toInt() and 255) shl 8)
        if (type != 5024) return
        observed++
        val body = frame.copyOfRange(4, frame.size - 2)
        bodyLengths[body.size] = (bodyLengths[body.size] ?: 0) + 1
        val info = Cirqa5024DeviceInformationParser.parse(body)
        if (info == null) rejected++ else {
            parsed++
            optionalLengths[info.optionalTrailingBytes] =
                (optionalLengths[info.optionalTrailingBytes] ?: 0) + 1
        }
    }

    fun summary(): String =
        "5024-Layout (Connect-5.31-Hypothese): Frames=$observed; " +
            "geparst=$parsed; nicht-passend=$rejected; Body-Längen=$bodyLengths; " +
            "optionale Restlängen=$optionalLengths; Identität/Strings=unterdrückt; " +
            "Abweichung ist kein Beweis für ungültige Geräteinformation"
}
