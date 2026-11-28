package de.pulselibre

/**
 * #737: activity status of the band, as Garmin Connect reads it (reference run 28.11.2026, see
 * docs/protocol/research/ISSUE737_ACTIVITY_STATUS_PROTOCOL_2026-11-28.md).
 *
 * Connect subscribes with `Smart{30:{1:{1:{1:19}}}}`; the band answers 30.2 and then sends 30.3 messages. A state
 * message carries `30.3 -> field 1012 -> field 2 -> field 1`; observed once: 3 running, 1 stopped, 5 saving,
 * 4 saved/ready. This class only SUBSCRIBES and READS – it sends no stop/save command. The meaning of the codes is
 * provisional until a second run confirms it, so [State.UNKNOWN] is used for anything not observed.
 */
object CirqaActivityStatus {
    const val SMART_FIELD = 30
    private const val STATE_CONTAINER = 1012

    enum class State(val code: Int?) { RUNNING(3), STOPPED(1), SAVING(5), READY(4), UNKNOWN(null) }

    /** Smart{30:{1:{1:{1:19}}}} – exact bytes of the Connect subscription. */
    fun subscribe(): ByteArray = byteArrayOf(0xF2.toByte(), 0x01, 0x06, 0x0A, 0x04, 0x0A, 0x02, 0x08, 0x13)

    fun mlValue(handle: Int, requestId: Int): ByteArray {
        require(handle in 1..127)
        return byteArrayOf(handle.toByte()) +
            GarminFileListRequest.cobs(CirqaFileAccessListRequest.gfdiFrame(requestId, subscribe()))
    }

    /** Raw state code of a 30.3 state message, or null for other messages (e.g. the per-second statistics). */
    fun stateCode(smart: ByteArray): Int? {
        val outer = CirqaFileAccessItemListParser.fields(smart) ?: return null
        val service = outer.firstOrNull { it.number == SMART_FIELD && it.wire == 2 }?.bytes ?: return null
        val update = CirqaFileAccessItemListParser.fields(service)?.firstOrNull { it.number == 3 && it.wire == 2 }?.bytes ?: return null
        val container = CirqaFileAccessItemListParser.fields(update)?.firstOrNull { it.number == STATE_CONTAINER && it.wire == 2 }?.bytes ?: return null
        val state = CirqaFileAccessItemListParser.fields(container)?.firstOrNull { it.number == 2 && it.wire == 2 }?.bytes ?: return null
        return CirqaFileAccessItemListParser.fields(state)?.firstOrNull { it.number == 1 && it.wire == 0 }?.varint?.toInt()
    }

    /** True for the band's subscription answer 30.2. */
    fun isSubscribed(smart: ByteArray): Boolean {
        val outer = CirqaFileAccessItemListParser.fields(smart) ?: return false
        val service = outer.firstOrNull { it.number == SMART_FIELD && it.wire == 2 }?.bytes ?: return false
        return CirqaFileAccessItemListParser.fields(service)?.any { it.number == 2 && it.wire == 2 } == true
    }

    fun state(code: Int?): State = State.entries.firstOrNull { it.code != null && it.code == code } ?: State.UNKNOWN
}
