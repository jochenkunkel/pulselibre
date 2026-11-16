package de.pulselibre

/**
 * Pure GFDI compact transport encoder, matching GarminHRM600 gfdi.py.
 * This class does not send BLE commands. CIRQA capability negotiation is pending.
 */
object GarminCompactTransport {
    fun frame(kind: Int, sequence: Int, counter: Int, protobuf: ByteArray): ByteArray {
        require(kind in 0..255)
        require(sequence in 1..127)
        require(counter in 0..65535)
        require(protobuf.size <= 65535 - 8)
        val type = 0x8000 or (sequence shl 8) or kind
        val length = protobuf.size + 8
        val prefix = byteArrayOf(
            length.toByte(), (length ushr 8).toByte(),
            type.toByte(), (type ushr 8).toByte(),
            counter.toByte(), (counter ushr 8).toByte()
        ) + protobuf
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    /** Sequence 1..127, counter 0..65535, with wraparound. */
    class Ids(startSequence: Int = 10, startCounter: Int = 1) {
        init {
            require(startSequence in 1..127)
            require(startCounter in 0..65535)
        }
        var sequence = startSequence
            private set
        var counter = startCounter
            private set

        fun next(): Pair<Int, Int> {
            val current = sequence to counter
            sequence = if (sequence == 127) 1 else sequence + 1
            counter = (counter + 1) and 0xffff
            return current
        }
    }
}
