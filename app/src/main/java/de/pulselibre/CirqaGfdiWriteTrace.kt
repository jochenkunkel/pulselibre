package de.pulselibre

/**
 * Bounded, redacted metadata for already-authorized GATT writes in the existing
 * diagnostic path. Never retains or returns request bodies or raw bytes.
 */
class CirqaGfdiWriteTrace(private val maxEvents: Int = 12) {
    init {
        require(maxEvents > 0)
    }

    private data class Entry(val requestShape: String, var callback: String = "PENDING")
    private val entries = mutableListOf<Entry>()
    private var omitted = 0
    private var pendingCallbackIndex: Int? = null

    fun record(gattValue: ByteArray, writeType: String, elapsedMillis: Long) {
        if (entries.size >= maxEvents) {
            omitted++
            return
        }
        val shape = "TX +${elapsedMillis.coerceAtLeast(0)}ms · ${describe(gattValue)} · " +
            "GATT-Wert=${gattValue.size}B · $writeType"
        entries += Entry(shape)
        pendingCallbackIndex = entries.lastIndex
    }

    fun completePendingCallback(status: Int) {
        val index = pendingCallbackIndex ?: return
        entries[index].callback =
            if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) "GATT-callback=SUCCESS"
            else "GATT-callback=ERROR($status)"
        pendingCallbackIndex = null
    }

    fun summary(): String = if (entries.isEmpty()) {
        "GATT-TX-Metadaten: keine Schreibvorgänge erfasst."
    } else {
        "GATT-TX-Metadaten (Payloads unterdrückt; ${entries.size} erfasst, $omitted wegen Limit ausgelassen):\n" +
            entries.joinToString("\n") { "${it.requestShape} · ${it.callback}" }
    }

    private fun describe(value: ByteArray): String {
        if (value.isEmpty()) return "leerer Wert"
        if (value[0] == 0.toByte() && value.size == 13 && value[1] == 0.toByte()) {
            val clientIdIsTwo = value[2] == 2.toByte() &&
                (3..9).all { value[it] == 0.toByte() }
            if (clientIdIsTwo) {
                val service = u16(value, 10)
                return "ML-REGISTER(service=$service)"
            }
        }
        if (value[0] == 0.toByte() && value.size >= 2) {
            return "ML-Steuerkanal(type=${value[1].toInt() and 0xff})"
        }
        if (value.size < 5 || value[1] != 0.toByte() || value.last() != 0.toByte()) {
            return "ML-Datenkanal(handle=${value[0].toInt() and 0xff}, GFDI=nicht verifiziert)"
        }
        val decoded = decodeCobs(value.copyOfRange(2, value.size - 1))
            ?: return "ML-Datenkanal(handle=${value[0].toInt() and 0xff}, COBS=ungültig)"
        val header = GarminGfdiHeader.inspect(decoded)
            ?: return "ML-Datenkanal(handle=${value[0].toInt() and 0xff}, GFDI/CRC=ungültig)"
        return "ML-Datenkanal(handle=${value[0].toInt() and 0xff}, GFDI-Typ=${header.type}, CRC=OK)"
    }

    private fun decodeCobs(encoded: ByteArray): ByteArray? {
        val decoded = ArrayList<Byte>(encoded.size)
        var index = 0
        while (index < encoded.size) {
            val code = encoded[index++].toInt() and 0xff
            if (code == 0 || index + code - 1 > encoded.size) return null
            repeat(code - 1) { decoded += encoded[index++] }
            if (code != 0xff && index < encoded.size) decoded += 0
        }
        return decoded.toByteArray()
    }

    private fun u16(bytes: ByteArray, offset: Int) =
        (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8)
}
