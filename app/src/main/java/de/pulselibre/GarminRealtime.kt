package de.pulselibre

/**
 * Pure, offline Garmin Multi-Link realtime payload decoding.
 * Service handles are assigned by the device; never infer a service from the handle number.
 * The routing byte is stripped exactly once before calling these parsers.
 *
 * Protocol layouts independently implemented from Gadgetbridge CommunicatorV2 and
 * garmin-ble metrics/heart_rate.py, metrics/steps.py.
 */
object GarminRealtime {
    const val HEART_RATE_SERVICE = 6
    const val STEPS_SERVICE = 7

    sealed class Reading {
        data class HeartRate(val bpm: Int, val restingBpm: Int?) : Reading()
        data class Steps(val count: Long, val goal: Long) : Reading()
    }

    /** Watch notifications use either a flagged or bare ML handle. */
    data class Routed(val handle: Int, val payload: ByteArray)

    fun route(packet: ByteArray): Routed? {
        if (packet.size < 2) return null
        val first = packet[0].toInt() and 0xff
        if (first == 0) return null
        val handle = if (first and 0x80 != 0) (first and 0x70) ushr 4 else first
        if (handle == 0) return null
        return Routed(handle, packet.copyOfRange(1, packet.size))
    }

    /** Decode only a service explicitly associated with a successfully registered handle. */
    fun decode(service: Int, payload: ByteArray): Reading? = when (service) {
        HEART_RATE_SERVICE -> {
            if (payload.size < 3) null
            else {
                val bpm = payload[1].toInt() and 0xff
                val resting = payload[2].toInt() and 0xff
                if (bpm == 0 || bpm > 240) null
                else Reading.HeartRate(bpm, resting.takeIf { it in 1..240 })
            }
        }
        STEPS_SERVICE -> {
            if (payload.size < 8) null
            else {
                fun le32(offset: Int): Long =
                    (0..3).fold(0L) { value, i ->
                        value or ((payload[offset + i].toLong() and 0xffL) shl (8 * i))
                    }
                Reading.Steps(le32(0), le32(4))
            }
        }
        else -> null
    }

    /** No service may be decoded until its dynamic handle has been registered. */
    class Registry {
        private val serviceByHandle = mutableMapOf<Int, Int>()
        fun register(service: Int, handle: Int): Boolean {
            if (service !in setOf(HEART_RATE_SERVICE, STEPS_SERVICE) || handle !in 1..127) return false
            serviceByHandle.entries.removeAll { it.value == service }
            serviceByHandle[handle] = service
            return true
        }
        fun clear() = serviceByHandle.clear()
        fun receive(packet: ByteArray): Reading? {
            val routed = route(packet) ?: return null
            val service = serviceByHandle[routed.handle] ?: return null
            return decode(service, routed.payload)
        }
    }
}
