package de.pulselibre

/** Pure protocol packets and deterministic handshake transitions; no Bluetooth dependencies. */
object GarminMlHandshake {
    const val GFDI_SERVICE = 1
    const val REGISTRATION_SERVICE = 4
    // Client ID 2 is used by the reviewed open-source Garmin ML implementation.
    fun registerRequest(service: Int): ByteArray {
        require(service in 1..32767)
        return byteArrayOf(0, 0, 2, 0, 0, 0, 0, 0, 0, 0,
            (service and 255).toByte(), (service ushr 8).toByte(), 0)
    }
    fun closeAllRequest(): ByteArray =
        byteArrayOf(0, 5, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0)

    enum class Stage { IDLE, WAIT_CLOSE, WAIT_REGISTER, COMPLETE, FAILED }
    class Session(private val closeAll: Boolean) {
        var stage = Stage.IDLE
            private set
        fun begin(): ByteArray {
            check(stage == Stage.IDLE)
            stage = if (closeAll) Stage.WAIT_CLOSE else Stage.WAIT_REGISTER
            return if (closeAll) closeAllRequest() else registerRequest(GFDI_SERVICE)
        }
        fun onNotification(packet: ByteArray): ByteArray? {
            if (stage == Stage.WAIT_CLOSE && packet.size >= 2 &&
                packet[0] == 0.toByte() && packet[1] == 6.toByte()) {
                stage = Stage.WAIT_REGISTER
                return registerRequest(GFDI_SERVICE)
            }
            if (stage == Stage.WAIT_REGISTER) {
                val response = GarminMlOffline.inspectRegisterResponse(packet) ?: return null
                if (response.serviceId != GFDI_SERVICE) return null
                stage = if (response.accepted) Stage.COMPLETE else Stage.FAILED
            }
            return null
        }
    }
}
