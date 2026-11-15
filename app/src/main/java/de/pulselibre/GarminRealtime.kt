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
    const val CALORIES_SERVICE = 8
    const val INTENSITY_SERVICE = 10
    const val HRV_SERVICE = 12
    const val STRESS_SERVICE = 13
    const val SPO2_SERVICE = 19
    const val BODY_BATTERY_SERVICE = 20
    const val RESPIRATION_SERVICE = 21
    val SUPPORTED_SERVICES = listOf(HEART_RATE_SERVICE, STEPS_SERVICE, CALORIES_SERVICE,
        INTENSITY_SERVICE, HRV_SERVICE, STRESS_SERVICE, SPO2_SERVICE,
        BODY_BATTERY_SERVICE, RESPIRATION_SERVICE)

    sealed class Reading {
        data class HeartRate(val bpm: Int, val restingBpm: Int?) : Reading()
        data class Steps(val count: Long, val goal: Long) : Reading()
        data class Calories(val total: Long, val active: Long) : Reading()
        data class Intensity(val moderate: Int, val vigorous: Int) : Reading()
        data class Hrv(val rrMs: Int) : Reading()
        data class Stress(val level: Int) : Reading()
        data class SpO2(val percent: Int) : Reading()
        data class BodyBattery(val level: Int) : Reading()
        data class Respiration(val breathsPerMinute: Int) : Reading()
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

    private fun u16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 255) or ((bytes[offset + 1].toInt() and 255) shl 8)

    private fun u32(bytes: ByteArray, offset: Int): Long =
        (0..3).fold(0L) { value, i ->
            value or ((bytes[offset + i].toLong() and 255L) shl (8 * i))
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
                Reading.Steps(u32(payload, 0), u32(payload, 4))
            }
        }
        CALORIES_SERVICE -> if (payload.size < 8) null else
            Reading.Calories(u32(payload, 0), u32(payload, 4))
        INTENSITY_SERVICE -> if (payload.size < 4) null else
            Reading.Intensity(u16(payload, 0), u16(payload, 2))
        HRV_SERVICE -> if (payload.size < 2) null else
            u16(payload, 0).takeIf { it > 0 }?.let { Reading.Hrv(it) }
        STRESS_SERVICE -> payload.firstOrNull()?.toInt()?.takeIf { it in 0..100 }
            ?.let { Reading.Stress(it) }
        SPO2_SERVICE -> payload.firstOrNull()?.toInt()?.and(255)
            ?.takeIf { it in 1..100 }?.let { Reading.SpO2(it) }
        BODY_BATTERY_SERVICE -> payload.firstOrNull()?.toInt()?.takeIf { it in 0..100 }
            ?.let { Reading.BodyBattery(it) }
        RESPIRATION_SERVICE -> payload.firstOrNull()?.toInt()?.takeIf { it in 1..100 }
            ?.let { Reading.Respiration(it) }
        else -> null
    }

    /** No service may be decoded until its dynamic handle has been registered. */
    class Registry {
        private val serviceByHandle = mutableMapOf<Int, Int>()
        fun register(service: Int, handle: Int): Boolean {
            if (service !in SUPPORTED_SERVICES || handle !in 1..127) return false
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
