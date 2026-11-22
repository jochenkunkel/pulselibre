package de.pulselibre

/**
 * Fail-closed recognition of `Smart.core_service.sync_response` on the 5044
 * protobuf path. Confirmed device shape: `13:2[2:2]` (Smart field 13,
 * CoreService field 2). Only field numbers and wire types are inspected; no
 * values are read, logged or echoed. Messages that are not this exact nested
 * structure (for example a file_access_service item_list_response) are rejected.
 */
object CirqaSyncResponseParser {
    private const val SMART_CORE_SERVICE_FIELD = 13
    private const val CORE_SERVICE_SYNC_RESPONSE_FIELD = 2
    private const val MAX_FIELDS = 32

    fun isSyncResponse(message: ByteArray): Boolean {
        if (message.isEmpty()) return false
        val coreService = lengthDelimitedField(message, SMART_CORE_SERVICE_FIELD) ?: return false
        val syncResponse = lengthDelimitedField(coreService, CORE_SERVICE_SYNC_RESPONSE_FIELD)
            ?: return false
        return syncResponse.isNotEmpty()
    }

    /** Returns the content of the first length-delimited field with [wanted]. */
    private fun lengthDelimitedField(bytes: ByteArray, wanted: Int): ByteArray? {
        var i = 0
        var seen = 0
        while (i < bytes.size && seen++ < MAX_FIELDS) {
            val key = readVarint(bytes, i) ?: return null
            i = key.second
            val field = (key.first ushr 3).toInt()
            val wire = (key.first and 7).toInt()
            if (field == 0 || wire !in 0..5 || wire == 3 || wire == 4) return null
            when (wire) {
                0 -> {
                    val value = readVarint(bytes, i) ?: return null
                    i = value.second
                }
                1 -> i += 8
                5 -> i += 4
                2 -> {
                    val length = readVarint(bytes, i) ?: return null
                    i = length.second
                    val end = i + length.first.toInt()
                    if (end < i || end > bytes.size) return null
                    if (field == wanted) return bytes.copyOfRange(i, end)
                    i = end
                }
                else -> return null
            }
            if (i > bytes.size) return null
        }
        return null
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
