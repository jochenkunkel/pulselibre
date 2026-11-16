package de.pulselibre

/** Bounded, metadata-only packet-length histogram for CIRQA diagnostics. */
internal class BoundedPacketLengths(private val maxDistinct: Int = 64) {
    init { require(maxDistinct > 0) }
    private val counts = mutableMapOf<Int, Int>()
    private var overflow = 0

    fun record(length: Int) {
        require(length >= 0)
        if (counts.containsKey(length)) {
            counts[length] = (counts[length] ?: 0).let { if (it == Int.MAX_VALUE) it else it + 1 }
        } else if (counts.size < maxDistinct) {
            counts[length] = 1
        } else if (overflow < Int.MAX_VALUE) {
            overflow++
        }
    }

    fun clear() {
        counts.clear()
        overflow = 0
    }

    fun summary(): String =
        counts.toSortedMap().entries.joinToString { "${it.key}:${it.value}" }.take(300) +
            if (overflow > 0) " (weitere Pakete: $overflow)" else ""
}
