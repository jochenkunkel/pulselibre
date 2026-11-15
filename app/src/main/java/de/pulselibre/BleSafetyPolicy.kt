package de.pulselibre

/**
 * Read-only BLE discovery policy until the wearable protocol is verified.
 * Never pair, bond, reset, write descriptors, or send proprietary commands.
 */
object BleSafetyPolicy {
    const val scanDurationMillis = 10_000L
    const val maximumDiagnosticBytes = 64

    fun mayReadCharacteristic(properties: Int): Boolean =
        properties and 0x02 != 0 // BluetoothGattCharacteristic.PROPERTY_READ

    fun maySubscribeWithoutProtocolApproval(): Boolean = false
    fun mayWriteWithoutProtocolApproval(): Boolean = false

    fun safeHex(bytes: ByteArray): String {
        val prefix = bytes.take(maximumDiagnosticBytes)
            .joinToString(" ") { "%02X".format(it.toInt() and 0xff) }
        return if (bytes.size > maximumDiagnosticBytes) "$prefix … (${bytes.size} Bytes)" else prefix
    }
}
