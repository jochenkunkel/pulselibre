package de.pulselibre

/**
 * Offline-only reader for the Connect/FileAccess item list protobuf.
 *
 * Wire path: Smart.file_access_service(43) -> Service.item_list_response(10)
 * -> ItemListResponse.items(4) -> ItemReference{uid(1), data_type(2),
 * data_size(3), flags(4)}. Identifiers are kept in memory only for a bounded
 * read-only pull request; reports use sizes/counts, never UID bytes.
 */
object CirqaFileAccessItemListParser {
    data class Field(val number: Int, val wire: Int, val varint: Long, val bytes: ByteArray?)

    data class Item(
        val uid: ByteArray,
        val dataType: ByteArray,
        val dataSize: Long,
        val flags: Long
    )

    fun fields(bytes: ByteArray): List<Field> {
        val out = ArrayList<Field>()
        var i = 0
        while (i < bytes.size && out.size < 512) {
            val key = readVarint(bytes, i) ?: break
            i = key.second
            val number = (key.first ushr 3).toInt()
            val wire = (key.first and 7).toInt()
            if (number == 0) break
            when (wire) {
                0 -> {
                    val v = readVarint(bytes, i) ?: break
                    out.add(Field(number, wire, v.first, null))
                    i = v.second
                }
                1 -> {
                    if (i + 8 > bytes.size) break
                    out.add(Field(number, wire, 0, bytes.copyOfRange(i, i + 8)))
                    i += 8
                }
                2 -> {
                    val len = readVarint(bytes, i) ?: break
                    val start = len.second
                    val end = start + len.first.toInt()
                    if (end > bytes.size) break
                    out.add(Field(number, wire, 0, bytes.copyOfRange(start, end)))
                    i = end
                }
                5 -> {
                    if (i + 4 > bytes.size) break
                    out.add(Field(number, wire, 0, bytes.copyOfRange(i, i + 4)))
                    i += 4
                }
                else -> break
            }
        }
        return out
    }

    /** Extract the item list from a complete reassembled Smart protobuf. */
    fun parse(smart: ByteArray): List<Item> {
        val service = fields(smart).firstOrNull { it.number == 43 && it.wire == 2 }?.bytes ?: return emptyList()
        val response = fields(service).firstOrNull { it.number == 10 && it.wire == 2 }?.bytes ?: return emptyList()
        return fields(response).filter { it.number == 4 && it.wire == 2 }.mapNotNull { entry ->
            val f = fields(entry.bytes ?: return@mapNotNull null)
            val uid = f.firstOrNull { it.number == 1 && it.wire == 2 }?.bytes ?: return@mapNotNull null
            val dataType = f.firstOrNull { it.number == 2 && it.wire == 2 }?.bytes ?: byteArrayOf()
            val size = f.firstOrNull { it.number == 3 && it.wire == 0 }?.varint ?: 0L
            val flags = f.firstOrNull { it.number == 4 && it.wire == 0 }?.varint ?: 0L
            Item(uid, dataType, size, flags)
        }
    }

    data class PullResult(
        val resultCode: Long,
        val transport: Long,
        val transferHandle: Long,
        val hasFilePath: Boolean,
        val compressionWindow: Long?
    )

    /** Extract Service.pull_item_response(2) -> PullItemDataResponse from a Smart message. */
    fun parsePullResult(smart: ByteArray): PullResult? {
        val service = fields(smart).firstOrNull { it.number == 43 && it.wire == 2 }?.bytes ?: return null
        val response = fields(service).firstOrNull { it.number == 2 && it.wire == 2 }?.bytes ?: return null
        val f = fields(response)
        fun v(number: Int) = f.firstOrNull { it.number == number && it.wire == 0 }?.varint
        return PullResult(
            resultCode = v(1) ?: return null,
            transport = v(2) ?: return null,
            transferHandle = v(3) ?: return null,
            hasFilePath = f.any { it.number == 4 && it.wire == 2 },
            compressionWindow = v(8)
        )
    }

    fun redactedSummary(items: List<Item>): String =
        "items=${items.size}; sizes=${items.take(12).joinToString(",") { it.dataSize.toString() }}"

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
