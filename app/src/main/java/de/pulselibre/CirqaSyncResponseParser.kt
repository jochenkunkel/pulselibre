package de.pulselibre

/** Structural validator for Smart.core_service.sync_response (CoreService field 2). */
internal object CirqaSyncResponseParser {
    /**
     * Returns true only for one complete Smart.core_service field containing
     * exactly one well-formed sync_response arm. Its nested body may be empty:
     * protobuf message fields with no set values are structurally valid.
     *
     * The captured device evidence does not establish nested response values or
     * status semantics, so this parser deliberately does not infer a result code.
     */
    fun isSyncResponse(smart: ByteArray): Boolean {
        val top = CirqaFileAccessItemListParser.fields(smart) ?: return false
        val coreServices = top.filter { it.number == CORE_SERVICE_FIELD && it.wire == 2 }
        if (coreServices.size != 1) return false
        val core = coreServices.single().bytes ?: return false
        val coreFields = CirqaFileAccessItemListParser.fields(core) ?: return false
        val responses = coreFields.filter {
            it.number == SYNC_RESPONSE_FIELD && it.wire == 2
        }
        if (responses.size != 1) return false
        val response = responses.single().bytes ?: return false
        return CirqaFileAccessItemListParser.fields(response) != null
    }

    private const val CORE_SERVICE_FIELD = 13
    private const val SYNC_RESPONSE_FIELD = 2
}
