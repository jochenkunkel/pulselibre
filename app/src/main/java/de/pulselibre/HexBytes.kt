package de.pulselibre

private val HEX_DIGITS = "0123456789abcdef".toCharArray()

/**
 * Lowercase hex without String.format. `"%02x".format(b)` parses its format string with a regex on every
 * byte; on the S10 that made decoding one 366 KB FIT file (≈27k observations, 123k raw fields) take ~56 s.
 */
fun ByteArray.lowerHex(): String {
    val out = CharArray(size * 2)
    for (i in indices) {
        val v = this[i].toInt() and 0xff
        out[2 * i] = HEX_DIGITS[v ushr 4]
        out[2 * i + 1] = HEX_DIGITS[v and 0x0f]
    }
    return String(out)
}

fun List<Byte>.lowerHex(): String = toByteArray().lowerHex()
