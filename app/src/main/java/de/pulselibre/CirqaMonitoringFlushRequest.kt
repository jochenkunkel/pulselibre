package de.pulselibre

/**
 * #637: Garmin Connect sends `Smart{47:{3:{1:0}}}` in every sync. In both decoded Connect captures (22.11. and
 * 24.11.2026) the band answered with `Smart{47:{4:{1:0}}}` and immediately afterwards pushed a NEW monitoring_b file
 * (FileAccess 12). PulsLibre never sent it, and on 27.11. the band offered no new daily file for hours of wear.
 *
 * Exact bytes as captured, no identifiers. Expected effect: the band closes its current monitoring data into a file
 * and announces it; PulsLibre pulls it read-only (#732). Semantics beyond this observation are not documented;
 * no flags, archive or delete are involved. Sent at most once per sync session.
 */
object CirqaMonitoringFlushRequest {
    const val SMART_FIELD = 47

    /** Smart{47: {3: {1: 0}}} */
    fun smart(): ByteArray = byteArrayOf(0xFA.toByte(), 0x02, 0x04, 0x1A, 0x02, 0x08, 0x00)

    fun mlValue(handle: Int, requestId: Int): ByteArray {
        require(handle in 1..127)
        return byteArrayOf(handle.toByte()) +
            GarminFileListRequest.cobs(CirqaFileAccessListRequest.gfdiFrame(requestId, smart()))
    }

    /** True for the band's answer Smart{47:{4:{...}}}. */
    fun isResponse(smart: ByteArray): Boolean {
        val outer = CirqaFileAccessItemListParser.fields(smart) ?: return false
        val service = outer.firstOrNull { it.number == SMART_FIELD && it.wire == 2 }?.bytes ?: return false
        return CirqaFileAccessItemListParser.fields(service)?.any { it.number == 4 && it.wire == 2 } == true
    }
}
