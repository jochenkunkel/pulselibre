package de.pulselibre

/** Structural validator for Smart.core_service.sync_response (CoreService field 2). */
internal object CirqaSyncResponseParser {
    /**
     * Returns true only for a complete, canonical Smart message containing the
     * expected sync_response arm. The observed successful CIRQA response has an
     * empty SyncResponse body (proto3 defaults); no numeric status field is
     * documented or present in that response shape.
     */
    fun isSyncResponse(smart: ByteArray): Boolean {
        val top = CirqaFileAccessItemListParser.fields(smart) ?: return false
        val coreFields = top.filter { it.number == CORE_SERVICE_FIELD && it.wire == 2 }
        if (coreFields.size != 1) return false
        val core = coreFields.single().bytes ?: return false
        val coreFieldsParsed = CirqaFileAccessItemListParser.fields(core) ?: return false
        val responses = coreFieldsParsed.filter { it.number == SYNC_RESPONSE_FIELD && it.wire == 2 }
        if (responses.size != 1) return false
        val response = responses.single().bytes ?: return false
        return CirqaFileAccessItemListParser.fields(response) != null
    }

    private const val CORE_SERVICE_FIELD = 13
    private const val SYNC_RESPONSE_FIELD = 2
}
