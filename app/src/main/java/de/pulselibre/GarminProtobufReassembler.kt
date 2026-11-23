package de.pulselibre

/**
 * Read-only, in-memory reassembly of standard GFDI 5043/5044 protobuf transport.
 * Wire layout follows GarminHRM600 gfdi.py parse_protobuf_transport.
 * No BLE writes, file mutations, persistent health data, or logging of payload.
 */
class GarminProtobufReassembler(private val maxBytes: Int = 65536) {
    data class CompletedMessage(val requestId: Int, val messageType: Int, val payload: ByteArray)

    init { require(maxBytes in 1..1048576) }

    private var requestId: Int? = null
    private var messageType: Int? = null
    private var total = 0
    private val data = ArrayList<Byte>()
    var completed = 0
        private set
    var rejected = 0
        private set

    /** Returns a completed protobuf only for a contiguous, validated transfer. */
    fun accept(frame: ByteArray): ByteArray? = acceptMessage(frame)?.payload

    /** Same strict validation as [accept], retaining correlation metadata. */
    fun acceptMessage(frame: ByteArray): CompletedMessage? {
        if (GarminFrameValidator.inspectVerified(frame) == null || frame.size < 20) return null
        val type = u16(frame, 2)
        if (type != 5043 && type != 5044) return null
        val body = frame.copyOfRange(4, frame.size - 2)
        val id = u16(body, 0)
        val offset = u32(body, 2)
        val declaredTotal = u32(body, 6)
        val chunkSize = u32(body, 10)
        if (declaredTotal > maxBytes.toLong() || declaredTotal <= 0 ||
            chunkSize > body.size - 14 || chunkSize <= 0 ||
            offset > declaredTotal || chunkSize > declaredTotal - offset ||
            chunkSize != (body.size - 14).toLong()) {
            rejected++
            reset()
            return null
        }
        if (offset == 0L) {
            // A duplicate first chunk must not silently erase progress.
            if (requestId == id && messageType == type && total == declaredTotal.toInt() &&
                data.size >= chunkSize.toInt() &&
                data.subList(0, chunkSize.toInt()).toByteArray().contentEquals(body.copyOfRange(14, body.size))) {
                return null
            }
            // A new transfer may supersede an incomplete one, but must be visible in diagnostics.
            if (requestId != null && data.isNotEmpty()) rejected++
            reset()
            requestId = id
            messageType = type
            total = declaredTotal.toInt()
        }
        // Accept a retransmission only when its bytes match already assembled data.
        if (requestId == id && messageType == type && total == declaredTotal.toInt() &&
            offset < data.size.toLong() && offset != 0L &&
            chunkSize <= data.size.toLong() - offset) {
            val start = offset.toInt()
            if (data.subList(start, start + chunkSize.toInt()).toByteArray()
                    .contentEquals(body.copyOfRange(14, body.size))) return null
        }
        if (requestId != id || messageType != type || total != declaredTotal.toInt() ||
            offset != data.size.toLong()) {
            rejected++
            reset()
            return null
        }
        for (index in 14 until body.size) data.add(body[index])
        if (data.size != total) return null
        val result = CompletedMessage(id, type, data.toByteArray())
        completed++
        reset()
        return result
    }

    fun reset() {
        requestId = null
        messageType = null
        total = 0
        data.clear()
    }

    fun summary(): String = "Protobuf-Übertragungen vollständig: $completed; verworfen: $rejected"

    private fun u16(bytes: ByteArray, index: Int) =
        (bytes[index].toInt() and 255) or ((bytes[index + 1].toInt() and 255) shl 8)

    private fun u32(bytes: ByteArray, index: Int): Long =
        (0..3).fold(0L) { n, i -> n or ((bytes[index + i].toLong() and 255L) shl (8 * i)) }
}
