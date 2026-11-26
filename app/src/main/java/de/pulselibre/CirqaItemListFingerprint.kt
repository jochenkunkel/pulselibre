package de.pulselibre

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest

/**
 * Stable, privacy-reduced fingerprint for comparing item lists across sessions.
 *
 * It contains no raw UID bytes. The returned digest remains a linkable summary
 * of a device's list and must stay in the app's local diagnostic output.
 */
object CirqaItemListFingerprint {
    fun create(items: List<CirqaFileAccessItemListParser.Item>): String {
        val itemDigests = items.map { item ->
            val bytes = ByteArrayOutputStream().also { buffer ->
                DataOutputStream(buffer).use { data ->
                    data.writeInt(item.uid.size)
                    data.write(item.uid)
                    data.writeInt(item.dataType.size)
                    data.write(item.dataType)
                    data.writeLong(item.dataSize)
                    data.writeLong(item.flags)
                }
            }.toByteArray()
            sha256(bytes)
        }.sorted()
        return sha256(buildString {
            append(itemDigests.size)
            itemDigests.forEach { append(':').append(it) }
        }.toByteArray(Charsets.UTF_8))
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .lowerHex()
}
