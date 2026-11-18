package de.pulselibre

/**
 * Offline-only, redacted structural inspection of a protobuf message.
 *
 * Reports only top-level field numbers and wire types, never values, payload
 * bytes, timestamps or identifiers. Used to classify unexpected device-originated
 * 5043 protobuf messages without adopting or echoing any foreign bytes.
 */
object CirqaProtobufShapeInspector {
    /** e.g. "1:2,4:0,9:2" (field:wireType), bounded and failure-tolerant. */
    fun shape(bytes: ByteArray, maxFields: Int = 32): String {
        require(maxFields in 1..128)
        val out = ArrayList<String>()
        var i = 0
        var steps = 0
        loop@ while (i < bytes.size && out.size < maxFields && steps++ < 2048) {
            val key = readVarint(bytes, i) ?: break
            i = key.second
            val field = (key.first ushr 3).toInt()
            val wire = (key.first and 7).toInt()
            if (field == 0 || wire !in 0..5 || wire == 3 || wire == 4) break
            out.add("$field:$wire")
            i = when (wire) {
                0 -> {
                    val v = readVarint(bytes, i) ?: break@loop
                    v.second
                }
                1 -> i + 8
                2 -> {
                    val len = readVarint(bytes, i) ?: break@loop
                    len.second + len.first.toInt()
                }
                5 -> i + 4
                else -> break@loop
            }
            if (i < 0 || i > bytes.size) break
        }
        return out.joinToString(",")
    }

    private fun readVarint(bytes: ByteArray, start: Int): Pair<Long, Int>? {
        var result = 0L
        var shift = 0
        var i = start
        while (i < bytes.size && shift < 64) {
            val b = bytes[i++].toInt() and 0xff
            result = result or ((b and 0x7f).toLong() shl shift)
            if (b and 0x80 == 0) return result to i
            shift += 7
        }
        return null
    }
}
