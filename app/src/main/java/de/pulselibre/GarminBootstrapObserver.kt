package de.pulselibre

/**
 * Passive, bounded GFDI bootstrap observer. It never writes to the device.
 * A positive status alone is not proof that CIRQA supports FileSync V2.
 */
class GarminBootstrapObserver {
    enum class State { WAITING_FOR_DEVICE, DEVICE_INFORMATION_SEEN, FILESYNC_REJECTED, FILE_LIST_SEEN }
    var state = State.WAITING_FOR_DEVICE
        private set
    var deviceInformationFrames = 0
        private set
    var repeatedDeviceInformationFrames = 0
        private set
    var rejectedFileListRequests = 0
        private set
    var acknowledgedFileListRequests = 0
        private set

    /** Feed only CRC- and length-verified GFDI frames. */
    fun observe(frame: ByteArray) {
        if (GarminFrameValidator.inspectVerified(frame) == null || frame.size < 6) return
        val rawType = u16(frame, 2)
        val kind = if (rawType and 0x8000 != 0) 5000 + (rawType and 0xff) else rawType
        if (kind == 5024) {
            if (deviceInformationFrames > 0) repeatedDeviceInformationFrames++
            deviceInformationFrames++
            if (state == State.WAITING_FOR_DEVICE) state = State.DEVICE_INFORMATION_SEEN
        }
        if (kind == 5000 && frame.size >= 9) {
            val request = u16(frame, 4)
            val status = frame[6].toInt() and 255
            if (request == 0x8a39) {
                when (status) {
                    0 -> acknowledgedFileListRequests++
                    2 -> {
                        rejectedFileListRequests++
                        if (state != State.FILE_LIST_SEEN) state = State.FILESYNC_REJECTED
                    }
                }
            }
        }
    }

    /** Called only after a validated FileListResponse has been decoded. */
    fun observeFileListResponse() { state = State.FILE_LIST_SEEN }

    fun summary(): String =
        "Bootstrap: $state; Geräteinformationen: $deviceInformationFrames; wiederholte 5024: $repeatedDeviceInformationFrames; " +
        "Dateilisten-ACK: $acknowledgedFileListRequests; " +
        "Dateilisten-UNSUPPORTED: $rejectedFileListRequests"

    private fun u16(data: ByteArray, offset: Int) =
        (data[offset].toInt() and 255) or ((data[offset + 1].toInt() and 255) shl 8)
}
