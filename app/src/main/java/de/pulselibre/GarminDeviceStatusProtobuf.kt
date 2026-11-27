package de.pulselibre

/**
 * Minimal, dependency-free protobuf codec for the Garmin `GDIDeviceStatus` Smart extension (#142/#159).
 *
 * Wire facts verified against the local Garmin Connect 5.31 APK (decompiled descriptor):
 *  - Smart extension field 8 = `deviceStatusService` (length-delimited)
 *  - DeviceStatusService field 2 = `remote_device_battery_status_request` (empty message)
 *  - DeviceStatusService field 3 = `remote_device_battery_status_response`
 *  - Response fields: 1 = response status, 2 = current_battery_level, 3 = battery_status,
 *    4 = estimated_hours_remaining (all varints)
 *
 * Only reading the response is implemented; no field is invented when it is absent.
 */
internal object GarminDeviceStatusProtobuf {
    private const val SMART_DEVICE_STATUS_SERVICE = 8
    private const val SERVICE_BATTERY_REQUEST = 2
    private const val SERVICE_BATTERY_RESPONSE = 3
    private const val RESPONSE_STATUS = 1
    private const val RESPONSE_BATTERY_LEVEL = 2
    private const val RESPONSE_BATTERY_STATUS = 3
    private const val RESPONSE_HOURS = 4

    /** Garmin `RemoteDeviceBatteryStatusBatteryStatus` enum names. */
    const val STATUS_UNKNOWN = "UNKNOWN_STATUS"
    const val STATUS_NORMAL = "NORMAL"
    const val STATUS_LOW = "LOW"
    const val STATUS_CRITICAL = "CRITICAL"
    const val STATUS_CHARGING = "CHARGING"
    const val STATUS_FULLY_CHARGED = "FULLY_CHARGED"
    const val STATUS_ERROR = "ERROR"
    const val STATUS_LOW_PULSE_OX = "LOW_PULSE_OX"

    const val RESPONSE_OK = 1
    const val RESPONSE_NO_REMOTE_DEVICE = 2

    /** Empty Smart message carrying only the device-status battery request extension. */
    fun batteryStatusRequest(): ByteArray =
        bytesField(SMART_DEVICE_STATUS_SERVICE, bytesField(SERVICE_BATTERY_REQUEST, ByteArray(0)))

    data class BatteryResponse(
        val responseStatus: Int,
        val batteryLevel: Int?,
        val batteryStatus: Int?,
        val hoursRemaining: Int?
    ) {
        val isOk: Boolean get() = responseStatus == RESPONSE_OK
        val statusName: String get() = statusName(batteryStatus)
        val responseStatusName: String get() = responseStatusName(responseStatus)
    }

    /**
     * Stored values for a successful response (#532): level (%), status name and remaining hours, each only when the
     * band sent it. Shared by [DeviceMetadataProbe] and the history sync so both store identical rows.
     */
    fun toLiveMetadata(parsed: BatteryResponse, nowMs: Long): List<LiveDeviceMetadata> {
        if (!parsed.isOk || parsed.batteryLevel == null) return emptyList()
        return buildList {
            add(LiveDeviceMetadata(DeviceMetadataFields.BATTERY_LEVEL, parsed.batteryLevel.toString(), "%",
                DeviceMetadataSources.GARMIN_GDI_STATUS, nowMs))
            parsed.batteryStatus?.let { add(LiveDeviceMetadata(DeviceMetadataFields.BATTERY_STATUS, statusName(it), null,
                DeviceMetadataSources.GARMIN_GDI_STATUS, nowMs)) }
            parsed.hoursRemaining?.let { add(LiveDeviceMetadata(DeviceMetadataFields.BATTERY_HOURS, it.toString(), "h",
                DeviceMetadataSources.GARMIN_GDI_STATUS, nowMs)) }
        }
    }

    /** Parse the first device-status battery response found anywhere in a Smart container. */
    fun parseBatteryResponse(smart: ByteArray): BatteryResponse? {
        val services = lengthDelimitedFields(smart, SMART_DEVICE_STATUS_SERVICE)
        for (service in services) {
            val responses = lengthDelimitedFields(service, SERVICE_BATTERY_RESPONSE)
            for (response in responses) {
                val status = varintField(response, RESPONSE_STATUS)?.toInt() ?: continue
                return BatteryResponse(
                    responseStatus = status,
                    batteryLevel = varintField(response, RESPONSE_BATTERY_LEVEL)?.toInt(),
                    batteryStatus = varintField(response, RESPONSE_BATTERY_STATUS)?.toInt(),
                    hoursRemaining = varintField(response, RESPONSE_HOURS)?.toInt()
                )
            }
        }
        return null
    }

    /** True when the Smart container carries a battery-status response (even before full parsing). */
    fun containsBatteryResponse(smart: ByteArray): Boolean =
        lengthDelimitedFields(smart, SMART_DEVICE_STATUS_SERVICE).any { service ->
            lengthDelimitedFields(service, SERVICE_BATTERY_RESPONSE).isNotEmpty()
        }

    fun statusName(code: Int?): String = when (code) {
        0 -> STATUS_UNKNOWN
        1 -> STATUS_NORMAL
        2 -> STATUS_LOW
        3 -> STATUS_CRITICAL
        4 -> STATUS_CHARGING
        5 -> STATUS_FULLY_CHARGED
        6 -> STATUS_ERROR
        7 -> STATUS_LOW_PULSE_OX
        else -> STATUS_UNKNOWN
    }

    fun responseStatusName(code: Int): String = when (code) {
        RESPONSE_OK -> "OK"
        RESPONSE_NO_REMOTE_DEVICE -> "NO_REMOTE_DEVICE"
        else -> "UNKNOWN_RESPONSE_STATUS"
    }

    private fun bytesField(field: Int, bytes: ByteArray): ByteArray =
        varint((field.toLong() shl 3) or 2L) + varint(bytes.size.toLong()) + bytes

    private fun varint(value: Long): ByteArray {
        var remaining = value
        val out = ArrayList<Byte>(5)
        do {
            val b = (remaining and 127).toInt()
            remaining = remaining ushr 7
            out.add((if (remaining == 0L) b else b or 128).toByte())
        } while (remaining != 0L)
        return out.toByteArray()
    }

    /** First top-level varint value for a field, or null when absent. */
    private fun varintField(message: ByteArray, field: Int): Long? {
        var offset = 0
        while (offset < message.size) {
            val key = readVarint(message, offset) ?: return null
            offset = key.second
            val number = (key.first ushr 3).toInt()
            val wire = (key.first and 7).toInt()
            when (wire) {
                0 -> {
                    val value = readVarint(message, offset) ?: return null
                    if (number == field) return value.first
                    offset = value.second
                }
                1 -> offset += 8
                2 -> {
                    val len = readVarint(message, offset) ?: return null
                    offset = len.second + len.first.toInt()
                }
                5 -> offset += 4
                else -> return null
            }
            if (offset > message.size) return null
        }
        return null
    }

    /** All top-level length-delimited values for a field. */
    private fun lengthDelimitedFields(message: ByteArray, field: Int): List<ByteArray> {
        val result = ArrayList<ByteArray>()
        var offset = 0
        while (offset < message.size) {
            val key = readVarint(message, offset) ?: break
            offset = key.second
            val number = (key.first ushr 3).toInt()
            val wire = (key.first and 7).toInt()
            when (wire) {
                0 -> { val v = readVarint(message, offset) ?: break; offset = v.second }
                1 -> offset += 8
                2 -> {
                    val len = readVarint(message, offset) ?: break
                    val start = len.second
                    val end = start + len.first.toInt()
                    if (end > message.size) break
                    if (number == field) result.add(message.copyOfRange(start, end))
                    offset = end
                }
                5 -> offset += 4
                else -> break
            }
            if (offset > message.size) break
        }
        return result
    }

    /** Pair of (value, nextOffset); null on malformed/unterminated varint. */
    private fun readVarint(data: ByteArray, start: Int): Pair<Long, Int>? {
        var result = 0L
        var shift = 0
        var i = start
        while (i < data.size && shift < 64) {
            val b = data[i].toInt() and 0xff
            result = result or ((b and 0x7f).toLong() shl shift)
            i++
            if (b and 0x80 == 0) return result to i
            shift += 7
        }
        return null
    }
}
