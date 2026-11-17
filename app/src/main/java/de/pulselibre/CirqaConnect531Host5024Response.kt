package de.pulselibre

import java.io.ByteArrayOutputStream

/**
 * Offline reproduction of the host extension to Garmin Connect 5.31's
 * GFDI 5000 ACK for device information (5024).
 *
 * Inputs must be obtained from actual host configuration. This class never
 * chooses device identity, feature flags, app version or Android Build values.
 * It is intentionally not connected to the BLE sender.
 */
object CirqaConnect531Host5024Response {
    data class Host(
        val featureFlag: Boolean,
        val deviceProtocol: Int,
        val appVersion: Int,
        val friendlyName: String,
        val manufacturer: String,
        val model: String
    )

    fun extension(host: Host): ByteArray {
        require(host.deviceProtocol in 0..65535)
        require(host.appVersion in 0..65535)
        val out = ByteArrayOutputStream()
        fun u16(value: Int) {
            out.write(value and 255)
            out.write((value ushr 8) and 255)
        }
        fun u32AllOnes() = repeat(4) { out.write(255) }
        fun utf8(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            require(bytes.size <= 255) { "UTF-8 byte length exceeds u8 prefix" }
            out.write(bytes.size)
            out.write(bytes)
        }
        u16(if (host.featureFlag && host.deviceProtocol >= 150) 150 else 113)
        u16(65535)
        u32AllOnes()
        u16(host.appVersion)
        u16(65535)
        utf8(host.friendlyName)
        utf8(host.manufacturer)
        utf8(host.model)
        out.write(1) // Observed final byte; meaning unknown.
        return out.toByteArray()
    }

    fun frame(host: Host): ByteArray =
        GarminDeviceInformationAckCandidate.extendedFrame(extension(host))
}
