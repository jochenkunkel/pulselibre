package de.pulselibre

/**
 * CRC-16/ARC as reconstructed from Garmin Connect 5.31 u45.a.
 * DEX inspection confirms yw40 compares the updated accumulator after the final
 * payload byte. This is Connect evidence, not proof of CIRQA support. The bounded
 * live reader uses it only behind its explicit one-shot device-test gate.
 */
object CirqaLegacyCrc16 {
    fun compute(bytes: ByteArray, initial: Int = 0): Int {
        require(initial in 0..0xffff)
        var crc = initial
        for (byte in bytes) {
            crc = crc xor (byte.toInt() and 255)
            repeat(8) {
                crc = if (crc and 1 != 0) (crc ushr 1) xor 0xa001 else crc ushr 1
            }
        }
        return crc and 0xffff
    }

    fun littleEndian(bytes: ByteArray): ByteArray {
        val value = compute(bytes)
        return byteArrayOf(value.toByte(), (value ushr 8).toByte())
    }
}
