package de.pulselibre

import java.security.MessageDigest

/**
 * Redacted comparison of two read-only item-list responses.
 *
 * UID digests are ephemeral comparison keys only: this result contains counts,
 * never UID bytes or digests. This establishes same-session repeatability, not
 * cross-session UID stability or archive completeness.
 */
internal object CirqaItemListRepeatability {
    data class Result(
        val firstCount: Int,
        val secondCount: Int,
        val unchanged: Int,
        val changedMetadata: Int,
        val added: Int,
        val missing: Int,
        val duplicateUidsFirst: Int,
        val duplicateUidsSecond: Int
    )

    fun compare(
        first: List<CirqaFileAccessItemListParser.Item>,
        second: List<CirqaFileAccessItemListParser.Item>
    ): Result {
        val a = index(first)
        val b = index(second)
        val common = a.keys intersect b.keys
        return Result(
            firstCount = first.size,
            secondCount = second.size,
            unchanged = common.count { a[it] == b[it] },
            changedMetadata = common.count { a[it] != b[it] },
            added = (b.keys - a.keys).size,
            missing = (a.keys - b.keys).size,
            duplicateUidsFirst = first.size - a.size,
            duplicateUidsSecond = second.size - b.size
        )
    }

    private fun index(
        items: List<CirqaFileAccessItemListParser.Item>
    ): Map<String, String> = items.associate { item ->
        val uidDigest = sha256(item.uid)
        val typeLength = ByteArray(4) { index -> (item.dataType.size ushr (index * 8)).toByte() }
        val metadata = sha256(typeLength + item.dataType + longBytes(item.dataSize) + longBytes(item.flags))
        uidDigest to metadata
    }

    private fun longBytes(value: Long): ByteArray =
        ByteArray(8) { index -> (value ushr (index * 8)).toByte() }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
}
