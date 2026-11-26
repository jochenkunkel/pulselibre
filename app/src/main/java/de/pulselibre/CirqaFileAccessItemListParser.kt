package de.pulselibre

/**
 * Offline-only reader for the Connect/FileAccess item list protobuf.
 *
 * Wire path: Smart.file_access_service(43) -> Service.item_list_response(10)
 * -> ItemListResponse.items(4) -> ItemReference{uid(1), data_type(2),
 * data_size(3), flags(4)}. Identifiers are kept in memory only for a bounded
 * read-only pull request; reports use sizes/counts, never UID bytes.
 *
 * The parser is deliberately fail-closed: a malformed, truncated, oversized or
 * otherwise non-canonical protobuf does not become a seemingly successful page,
 * item or pull result. Partial data is never silently accepted as a complete
 * historical listing.
 */
object CirqaFileAccessItemListParser {
    data class Field(val number: Int, val wire: Int, val varint: Long, val bytes: ByteArray?)

    data class Item(
        val uid: ByteArray,
        val dataType: ByteArray,
        val dataSize: Long,
        val flags: Long,
        val transactionId: Long? = null,
        val modifiedTime: Long? = null,
        /** Exact bounded ItemReference, retained locally including unknown metadata. */
        val referenceBytes: ByteArray = byteArrayOf()
    )
    data class ItemListPage(
        val items: List<Item>,
        val status: Long?,
        val sessionId: Long?,
        val nextTransactionId: Long?,
        val gdxmlStrings: Map<Long, String>,
        /** True only when the parser reached the end of the input without truncation. */
        val complete: Boolean = false
    )
    data class PullResult(
        val resultCode: Long,
        val transport: Long,
        val transferHandle: Long,
        val hasFilePath: Boolean,
        val compressionWindow: Long?,
        /** Optional protocol diagnostics; never include file paths or payload bytes. */
        val errorCode: Int?,
        val errorSource: Long?,
        val dataSpecificErrorSource: Long?,
        val dataSpecificErrorCode: Int?
    )

    /** Maximum number of fields accepted before a message is considered non-canonical. */
    private const val MAX_FIELDS = 512

    /**
     * Parses a complete protobuf message. Returns null if the input is not a
     * complete, canonical protobuf with all declared lengths present. No partial
     * prefix is returned.
     */
    fun fields(bytes: ByteArray): List<Field>? {
        val out = ArrayList<Field>()
        var i = 0
        while (i < bytes.size) {
            if (out.size >= MAX_FIELDS) return null
            val key = readVarint(bytes, i) ?: return null
            i = key.second
            val number = (key.first ushr 3).toInt()
            val wire = (key.first and 7).toInt()
            if (number == 0) return null
            when (wire) {
                0 -> {
                    val v = readVarint(bytes, i) ?: return null
                    out.add(Field(number, wire, v.first, null))
                    i = v.second
                }
                1 -> {
                    if (i + 8 > bytes.size) return null
                    out.add(Field(number, wire, 0, bytes.copyOfRange(i, i + 8)))
                    i += 8
                }
                2 -> {
                    val len = readVarint(bytes, i) ?: return null
                    val length = len.first
                    if (length < 0 || length > Int.MAX_VALUE) return null
                    val start = len.second
                    val end = start + length.toInt()
                    if (end < start || end > bytes.size) return null
                    out.add(Field(number, wire, 0, bytes.copyOfRange(start, end)))
                    i = end
                }
                5 -> {
                    if (i + 4 > bytes.size) return null
                    out.add(Field(number, wire, 0, bytes.copyOfRange(i, i + 4)))
                    i += 4
                }
                else -> return null
            }
        }
        return out
    }

    /** Extract the item list from a complete reassembled Smart protobuf. */
    fun parse(smart: ByteArray): List<Item> = parsePage(smart)?.takeIf { it.complete }?.items.orEmpty()

    /** Page context is exposed only as presence bits; opaque IDs stay unreported. */
    fun parsePage(smart: ByteArray): ItemListPage? {
        val service = fields(smart)?.firstOrNull { it.number == 43 && it.wire == 2 }?.bytes ?: return null
        val response = fields(service)?.firstOrNull { it.number == 10 && it.wire == 2 }?.bytes ?: return null
        val responseFields = fields(response) ?: return null
        val gdxmlStrings = linkedMapOf<Long, String>()
        val items = ArrayList<Item>()
        for (entry in responseFields.filter { it.number == 4 && it.wire == 2 }) {
            items.add(parseItemReference(entry.bytes ?: return null, gdxmlStrings) ?: return null)
        }
        val sessionId = responseFields.firstOrNull { it.number == 2 && it.wire == 0 }?.varint
        val nextTransactionId = responseFields.firstOrNull { it.number == 3 && it.wire == 0 }?.varint
        if (sessionId != null && sessionId !in 0..0xffff_ffffL) return null
        if (nextTransactionId != null && nextTransactionId !in 0..0xffff_ffffL) return null
        return ItemListPage(
            items = items,
            status = responseFields.firstOrNull { it.number == 1 && it.wire == 0 }?.varint,
            sessionId = sessionId,
            nextTransactionId = nextTransactionId,
            gdxmlStrings = gdxmlStrings,
            complete = true
        )
    }

    /** One ItemReference{uid(1), data_type(2), data_size(3), flags(4), transaction_id(5), modified_time(7)}; fail-closed. */
    private fun parseItemReference(bytes: ByteArray, gdxmlStrings: MutableMap<Long, String>): Item? {
        val f = fields(bytes) ?: return null
        val uid = f.firstOrNull { it.number == 1 && it.wire == 2 }?.bytes ?: return null
        if (uid.isEmpty()) return null
        val dataType = f.firstOrNull { it.number == 2 && it.wire == 2 }?.bytes ?: byteArrayOf()
        val dataTypeFields = fields(dataType) ?: return null
        val typeLabel = dataTypeFields.firstOrNull { it.number == 2 && it.wire == 2 }
            ?.bytes?.toString(Charsets.UTF_8)
        val stringKey = dataTypeFields.firstOrNull { it.number == 3 && it.wire == 0 }?.varint
        if (typeLabel != null && stringKey != null) gdxmlStrings[stringKey] = typeLabel
        val size = f.firstOrNull { it.number == 3 && it.wire == 0 }?.varint ?: 0L
        val flags = f.firstOrNull { it.number == 4 && it.wire == 0 }?.varint ?: 0L
        val transaction = f.firstOrNull { it.number == 5 && it.wire == 0 }?.varint
        val modified = f.firstOrNull { it.number == 7 && it.wire == 0 }?.varint
        if (transaction != null && transaction !in 0..0xffff_ffffL) return null
        if (modified != null && modified !in 0..0xffff_ffffL) return null
        return Item(uid, dataType, size, flags, transaction, modified, bytes.copyOf())
    }

    /** A file the band announced on its own (#637). */
    data class ItemAvailable(val item: Item, val gdxmlStrings: Map<Long, String>)

    /**
     * Device-initiated Smart.file_access_service(43) -> field 12 {1: ItemReference}. In the Garmin Connect capture
     * (#637) the band sends this for every new monitoring (FIT 32/44), sleep (49) or other file while connected,
     * and Connect pulls the item right away. Read-only: parsing never triggers flags or deletion.
     */
    fun parseItemAvailable(smart: ByteArray): ItemAvailable? {
        val service = fields(smart)?.firstOrNull { it.number == 43 && it.wire == 2 }?.bytes ?: return null
        val notice = fields(service)?.firstOrNull { it.number == 12 && it.wire == 2 }?.bytes ?: return null
        val reference = fields(notice)?.firstOrNull { it.number == 1 && it.wire == 2 }?.bytes ?: return null
        val strings = linkedMapOf<Long, String>()
        val item = parseItemReference(reference, strings) ?: return null
        return ItemAvailable(item, strings)
    }

    /** Extract Service.pull_item_response(2) -> PullItemDataResponse from a Smart message. */
    fun parsePullResult(smart: ByteArray): PullResult? {
        val service = fields(smart)?.firstOrNull { it.number == 43 && it.wire == 2 }?.bytes ?: return null
        val response = fields(service)?.firstOrNull { it.number == 2 && it.wire == 2 }?.bytes ?: return null
        val f = fields(response) ?: return null
        fun v(number: Int) = f.firstOrNull { it.number == number && it.wire == 0 }?.varint
        fun signed32(number: Int): Int? = v(number)?.let { encoded ->
            val value = encoded.toInt()
            (value ushr 1) xor -(value and 1)
        }
        // proto3 omits default values, so SUCCESS=0 / transport=0 / handle=0
        // may be absent. A missing result field, however, means this is not a
        // pull response at all.
        if (!f.any { it.number == 1 && it.wire == 0 }) return null
        return PullResult(
            resultCode = v(1) ?: 0,
            transport = v(2) ?: 0,
            transferHandle = v(3) ?: 0,
            hasFilePath = f.any { it.number == 4 && it.wire == 2 },
            compressionWindow = v(7),
            errorCode = signed32(5),
            errorSource = v(6),
            dataSpecificErrorSource = v(8),
            dataSpecificErrorCode = signed32(9)
        )
    }

    fun redactedSummary(
        items: List<Item>,
        gdxmlStrings: Map<Long, String> = emptyMap()
    ): String =
        "items=${items.size}; types=${items.groupingBy { safeTypeLabel(it, gdxmlStrings) }.eachCount()
            .entries.joinToString(",") { "${it.key}:${it.value}" }}; " +
            "sizes=${items.take(12).joinToString(",") { it.dataSize.toString() }}"

    /** Returns only a schema-like GDXML label; never emits UIDs or arbitrary strings. */
    fun safeTypeLabel(item: Item, gdxmlStrings: Map<Long, String> = emptyMap()): String {
        val dataTypeFields = fields(item.dataType) ?: return "unknown"
        val format = dataTypeFields.firstOrNull { it.number == 1 && it.wire == 0 }?.varint
        val directLabel = dataTypeFields.firstOrNull { it.number == 2 && it.wire == 2 }
            ?.bytes?.toString(Charsets.UTF_8)
        val stringKey = dataTypeFields.firstOrNull { it.number == 3 && it.wire == 0 }?.varint
        val rawLabel = directLabel ?: stringKey?.let(gdxmlStrings::get)
        val label = rawLabel?.takeIf { it.matches(Regex("[A-Z0-9_]{1,48}")) }
        return when {
            label != null -> label
            format != null -> "format_$format"
            else -> "unknown"
        }
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
