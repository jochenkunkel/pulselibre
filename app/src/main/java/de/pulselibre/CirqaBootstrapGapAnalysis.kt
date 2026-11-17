package de.pulselibre

/**
 * Offline-only comparison of observed CIRQA bootstrap with a published
 * HRM600/fenix-watch emulation. Reference frames are not valid CIRQA commands.
 */
object CirqaBootstrapGapAnalysis {
    enum class NextStep {
        VERIFY_DEVICE_INFORMATION_ACK,
        VERIFY_POST_CONFIGURATION_BOOTSTRAP,
        INVESTIGATE_FILESYNC_VARIANT,
        FILE_LIST_CONFIRMED
    }

    data class Evidence(
        val deviceInformationReceived: Boolean,
        val minimal5024AckGattConfirmed: Boolean,
        val configuration5050Ack: Boolean,
        val synchronization5037Observed: Boolean,
        val fileListResponseObserved: Boolean,
        val compactFileListUnsupported: Boolean
    )

    fun next(e: Evidence): NextStep = when {
        e.fileListResponseObserved -> NextStep.FILE_LIST_CONFIRMED
        !e.deviceInformationReceived || !e.minimal5024AckGattConfirmed ->
            NextStep.VERIFY_DEVICE_INFORMATION_ACK
        !e.configuration5050Ack || !e.synchronization5037Observed ->
            NextStep.VERIFY_POST_CONFIGURATION_BOOTSTRAP
        else -> NextStep.INVESTIGATE_FILESYNC_VARIANT
    }

    /**
     * Human-readable diagnostic; does not claim that an application-level
     * 5024 acknowledgement was accepted merely because GATT confirmed writing.
     */
    fun explain(e: Evidence): String =
        when (next(e)) {
            NextStep.VERIFY_DEVICE_INFORMATION_ACK ->
                "5024-Handshake nicht vollständig belegt; GATT-Schreiben allein ist kein Anwendungs-ACK."
            NextStep.VERIFY_POST_CONFIGURATION_BOOTSTRAP ->
                "5050 bestätigt, aber kein 5037: zusätzliche Bootstrap-Voraussetzungen offen; " +
                    "HRM600-Wachemulation nicht ungeprüft auf CIRQA übertragen."
            NextStep.INVESTIGATE_FILESYNC_VARIANT ->
                "5037 gesehen, aber keine Dateiliste; FileSync-Variante prüfen."
            NextStep.FILE_LIST_CONFIRMED -> "Dateiliste protokollseitig bestätigt; Dateiabruf separat validieren."
        } + if (e.compactFileListUnsupported) " Frühere kompakte FileListRequest: UNSUPPORTED." else ""
}
