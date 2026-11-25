package de.pulselibre

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.ArrayDeque
import java.util.UUID

/**
 * Bounded, read-only device metadata query (#142/#159): standard GATT battery/device-info reads
 * first, then — only when no GATT battery level was obtained — a single Garmin `GDIDeviceStatus`
 * battery query over the ML data channel. Every observed value is returned for persistence.
 *
 * Stop gates: no pairing/unpairing, no reset, no CLOSE_ALL, no config/profile writes. The only
 * writes are the ML registration + battery-status query (an approved read-only query) and the
 * CCCD subscription needed to observe its response. No notifications are subscribed on GATT
 * services other than the Garmin ML data channel. No instrumentation tests run against the S25.
 */
@SuppressLint("MissingPermission")
internal class DeviceMetadataProbe(private val context: Context, private val adapter: BluetoothAdapter) {
    private val handler = Handler(Looper.getMainLooper())
    private var gatt: BluetoothGatt? = null
    private var active = false
    private var callback: ((ProbeResult) -> Unit)? = null
    private val collected = LinkedHashMap<String, LiveDeviceMetadata>()
    private val readQueue = ArrayDeque<BluetoothGattCharacteristic>()
    private var readInFlight = false
    private var gattBatterySeen = false
    private var notes = ArrayList<String>()

    private var rx: BluetoothGattCharacteristic? = null
    private var tx: BluetoothGattCharacteristic? = null
    private var dataHandle: Int? = null
    private var registered = false
    private var statusRequestSent = false
    private val notifications = ArrayList<ByteArray>()

    data class ProbeResult(val values: List<LiveDeviceMetadata>, val summary: String)

    fun start(result: (ProbeResult) -> Unit) {
        if (active) { result(ProbeResult(emptyList(), "Metadaten-Abfrage läuft bereits.")); return }
        val device: BluetoothDevice = try {
            adapter.bondedDevices.orEmpty().firstOrNull {
                it.name?.contains("CIRQA", ignoreCase = true) == true
            } ?: run {
                result(ProbeResult(emptyList(), "Kein gekoppeltes CIRQA gefunden; keine Verbindung aufgebaut."))
                return
            }
        } catch (_: SecurityException) {
            result(ProbeResult(emptyList(), "Bluetooth-Berechtigung fehlt.")); return
        }
        // One BLE session at a time: a second GATT client would subscribe the same ML channel and register GFDI again,
        // and the running history download would take that foreign registration response for its own.
        if (!CirqaBleSessionGate.acquire(this)) {
            result(ProbeResult(emptyList(), "CIRQA Bluetooth belegt; Metadaten-Abfrage übersprungen.")); return
        }
        active = true
        callback = result
        notes = ArrayList()
        try {
            // Callbacks on the main looper, like every timeout and finish(): all state stays on one thread.
            gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE,
                BluetoothDevice.PHY_LE_1M_MASK, handler)
            handler.postDelayed({ if (active) finish("Gesamt-Zeitlimit (40 s) erreicht.") }, TOTAL_TIMEOUT_MS)
        } catch (_: SecurityException) {
            finish("Bluetooth-Berechtigung fehlt.")
        } catch (_: IllegalArgumentException) {
            finish("GATT-Verbindung nicht möglich.")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (!active) { safeClose(g); return }
            if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                finish("GATT beendet: Status $status, Zustand $newState")
                return
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                try {
                    if (!g.discoverServices()) finish("Dienstsuche konnte nicht gestartet werden.")
                } catch (_: SecurityException) { finish("Berechtigung bei Dienstsuche entzogen.") }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (!active) return
            if (status != BluetoothGatt.GATT_SUCCESS) { finish("Dienstsuche fehlgeschlagen: $status"); return }
            val services = g.services.orEmpty()
            notes.add("Dienste=${services.size}")
            // Garmin ML pair for the optional status query fallback.
            val mlChars = services.firstOrNull {
                it.uuid.toString().equals(GarminMlOffline.SERVICE_UUID, true)
            }?.characteristics.orEmpty()
            val pair = GarminMlOffline.findCharacteristicPair(mlChars.map { it.uuid.toString() })
            if (pair != null) {
                val service = services.first { it.uuid.toString().equals(GarminMlOffline.SERVICE_UUID, true) }
                rx = service.getCharacteristic(UUID.fromString(pair.rx))
                tx = service.getCharacteristic(UUID.fromString(pair.tx))
            }
            // Read every readable characteristic, known ones first; bound the count.
            val all = services.flatMap { it.characteristics.orEmpty() }
                .filter { it.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0 }
            readQueue.clear()
            all.sortedByDescending { known(it.uuid.toString()) }.take(MAX_GATT_READS).forEach { readQueue.add(it) }
            notes.add("Lesbare Characteristics=${all.size}, geplant=${readQueue.size}")
            if (readQueue.isEmpty()) {
                afterGattReads()
            } else {
                readNext(g)
            }
        }

        @Deprecated("Android compatibility callback")
        override fun onCharacteristicRead(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            @Suppress("DEPRECATION")
            onRead(g, characteristic, status, characteristic.value?.clone() ?: byteArrayOf())
        }

        override fun onCharacteristicRead(
            g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int
        ) = onRead(g, characteristic, status, value)

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (!active || descriptor.uuid != CCCD_UUID) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                notes.add("CCCD-Fehler $status; kein Status-Request gesendet.")
                finish("GATT-Akku nicht verfügbar; Garmin-Status nicht abonnierbar.")
                return
            }
            // Never overlap GATT operations: the TX write must wait for the confirmed CCCD write.
            notes.add("CCCD bestätigt; sende ML-Registrierung")
            sendRegistration(g)
        }

        @Deprecated("Android compatibility callback")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            onNotification(g, characteristic.value?.clone() ?: byteArrayOf())
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) =
            onNotification(g, value)

        override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (!active) return
            if (status != BluetoothGatt.GATT_SUCCESS) notes.add("ML-Schreibfehler $status")
        }
    }

    private fun onRead(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int, value: ByteArray) {
        if (!active) return
        readInFlight = false
        if (status == BluetoothGatt.GATT_SUCCESS) {
            val now = System.currentTimeMillis()
            DeviceMetadataGattDecoder.decode(characteristic.uuid.toString(), value, now)?.let { entry ->
                collected[entry.fieldKey] = entry
                if (entry.fieldKey == DeviceMetadataFields.BATTERY_LEVEL &&
                    entry.source == DeviceMetadataSources.GATT_BATTERY) gattBatterySeen = true
            }
        } else {
            notes.add("Lese-Fehler ${characteristic.uuid}: $status")
        }
        if (readQueue.isNotEmpty()) readNext(g) else afterGattReads()
    }

    private fun readNext(g: BluetoothGatt) {
        if (!active || readInFlight) return
        val characteristic = readQueue.pollFirst() ?: run { afterGattReads(); return }
        readInFlight = true
        try {
            @Suppress("DEPRECATION")
            val ok = g.readCharacteristic(characteristic)
            if (!ok) { readInFlight = false; notes.add("Leseauftrag abgelehnt: ${characteristic.uuid}"); readNext(g) }
        } catch (_: SecurityException) {
            readInFlight = false
            finish("Berechtigung beim Lesen entzogen.")
        }
    }

    /** GATT first; the Garmin status query runs only when GATT offered no battery level. */
    private fun afterGattReads() {
        if (!active) return
        if (gattBatterySeen) {
            finish("GATT-Batterie ${collected[DeviceMetadataFields.BATTERY_LEVEL]?.valueText} % übernommen.")
            return
        }
        notes.add("Kein GATT-Akkustand; versuche Garmin-Gerätestatus.")
        val g = gatt ?: run { finish("Kein GATT; Gerätestatus nicht möglich."); return }
        val r = rx; val t = tx
        if (r == null || t == null || r.getDescriptor(CCCD_UUID) == null ||
            r.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0 ||
            t.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
            finish("Kein geeigneter Garmin-ML-Kanal für den Gerätestatus.")
            return
        }
        try {
            if (!g.setCharacteristicNotification(r, true)) { finish("ML-Benachrichtigungen nicht aktivierbar."); return }
            val descriptor = r.getDescriptor(CCCD_UUID)!!
            val ok = if (Build.VERSION.SDK_INT >= 33)
                g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS
            else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                g.writeDescriptor(descriptor)
            }
            if (!ok) finish("CCCD-Schreibauftrag abgelehnt.")
            else handler.postDelayed({ if (active && !registered) finish("Keine ML-Registrierung innerhalb von 18 s.") }, GDI_TIMEOUT_MS)
        } catch (_: SecurityException) { finish("Berechtigung beim Abonnieren entzogen.") }
    }

    /** Read-only handshake step: register the GFDI service; no CLOSE_ALL, reset or pairing. */
    private fun sendRegistration(g: BluetoothGatt) {
        val t = tx ?: run { finish("ML-TX fehlt."); return }
        writeBytes(g, t, GarminMlHandshake.registerRequest(GarminMlHandshake.GFDI_SERVICE))
    }

    private fun sendStatusRequest(g: BluetoothGatt) {
        if (!active || statusRequestSent) return
        val handle = dataHandle ?: run { finish("Kein Daten-Handle nach ML-Registrierung."); return }
        val t = tx ?: run { finish("ML-TX fehlt."); return }
        // Frame the actual device-status request — never reuse the FileList request builder here.
        val protobuf = GarminDeviceStatusProtobuf.batteryStatusRequest()
        val frame = GarminCompactTransport.frame(0x39, 10, 1, protobuf)
        val cobs = GarminFileListRequest.cobs(frame)
        val chunks = cobs.toList().chunked(ML_MTU_PAYLOAD).map { part ->
            byteArrayOf(handle.toByte()) + part.toByteArray()
        }
        if (chunks.isEmpty()) { finish("Status-Request konnte nicht gerahmt werden."); return }
        notes.add("Status-Request an Handle $handle, Frame=${frame.size} B, Chunks=${chunks.size}")
        statusRequestSent = true
        // WRITE_NO_RESPONSE does not reliably produce a write callback; space the chunks in time.
        chunks.forEachIndexed { index, bytes ->
            handler.postDelayed({ if (active) writeBytes(g, t, bytes) }, index.toLong() * CHUNK_GAP_MS)
        }
        handler.postDelayed({ if (active) finish("Keine Status-Antwort innerhalb von 18 s.") },
            GDI_TIMEOUT_MS + chunks.size * CHUNK_GAP_MS)
    }

    private fun writeBytes(g: BluetoothGatt, char: BluetoothGattCharacteristic, bytes: ByteArray) {
        val mode = if (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        try {
            val ok = if (Build.VERSION.SDK_INT >= 33)
                g.writeCharacteristic(char, bytes, mode) == BluetoothStatusCodes.SUCCESS
            else {
                char.writeType = mode
                @Suppress("DEPRECATION")
                char.value = bytes
                @Suppress("DEPRECATION")
                g.writeCharacteristic(char)
            }
            if (!ok) notes.add("ML-Schreibauftrag abgelehnt")
        } catch (_: SecurityException) { finish("Berechtigung beim Senden entzogen.") }
    }

    private fun onNotification(g: BluetoothGatt, value: ByteArray) {
        if (!active || value.isEmpty()) return
        if (!registered) {
            val response = GarminMlOffline.inspectRegisterResponse(value)
            if (response != null && response.serviceId == GarminMlHandshake.GFDI_SERVICE && response.accepted) {
                registered = true
                dataHandle = response.handle
                notes.add("GFDI registriert, Handle=${response.handle}")
                sendStatusRequest(g)
            }
            return
        }
        if (statusRequestSent) notifications.add(value.clone())
    }

    private fun known(uuid: String): Int {
        val k = uuid.lowercase(java.util.Locale.ROOT).let {
            if (it.startsWith("0000") && it.length >= 8) it.substring(4, 8) else it.take(4)
        }
        return when (k) {
            "2a19", "2a26", "2a27", "2a28", "2a24", "2a25", "2a29", "2a00", "2a2b" -> 1
            else -> 0
        }
    }

    @SuppressLint("MissingPermission")
    private fun safeClose(g: BluetoothGatt?) {
        if (g == null) return
        try { g.disconnect() } catch (_: Exception) {}
        try { g.close() } catch (_: Exception) {}
    }

    private fun finish(summary: String) {
        handler.post {
            if (!active) { safeClose(gatt); CirqaBleSessionGate.release(this); return@post }
            active = false
            handler.removeCallbacksAndMessages(null)
            // A late status response may still have arrived: decode before closing.
            if (statusRequestSent && notifications.isNotEmpty()) {
                val parsed = GarminDeviceStatusResponseDecoder.decode(notifications)
                if (parsed != null && parsed.isOk && parsed.batteryLevel != null) {
                    val now = System.currentTimeMillis()
                    val level = LiveDeviceMetadata(DeviceMetadataFields.BATTERY_LEVEL,
                        parsed.batteryLevel.toString(), "%", DeviceMetadataSources.GARMIN_GDI_STATUS, now)
                    collected[level.fieldKey] = level
                    parsed.batteryStatus?.let { code ->
                        val status = LiveDeviceMetadata(DeviceMetadataFields.BATTERY_STATUS,
                            GarminDeviceStatusProtobuf.statusName(code), null,
                            DeviceMetadataSources.GARMIN_GDI_STATUS, now)
                        collected[status.fieldKey] = status
                    }
                    parsed.hoursRemaining?.let { hours ->
                        val runtime = LiveDeviceMetadata(DeviceMetadataFields.BATTERY_HOURS,
                            hours.toString(), "h", DeviceMetadataSources.GARMIN_GDI_STATUS, now)
                        collected[runtime.fieldKey] = runtime
                    }
                    notes.add("Garmin-Status dekodiert: ${parsed.responseStatusName}")
                } else {
                    notes.add("Garmin-Status nicht dekodierbar (Notifications=${notifications.size}).")
                }
                // Keep every additional value the band offered, even when no battery response came.
                GarminDeviceStatusResponseDecoder.extraValues(notifications, System.currentTimeMillis())
                    .forEach { collected.putIfAbsent(it.fieldKey, it) }
            }
            safeClose(gatt)
            gatt = null
            CirqaBleSessionGate.release(this)
            val full = buildString {
                append(summary)
                notes.forEach { append(" | ").append(it) }
                append(" | Werte=").append(collected.size)
            }
            val cb = callback
            callback = null
            cb?.invoke(ProbeResult(collected.values.toList(), full))
        }
    }

    fun close() {
        active = false
        handler.removeCallbacksAndMessages(null)
        safeClose(gatt)
        gatt = null
        CirqaBleSessionGate.release(this)
        callback = null
    }

    private companion object {
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        const val TOTAL_TIMEOUT_MS = 40_000L
        const val GDI_TIMEOUT_MS = 18_000L
        const val MAX_GATT_READS = 32
        const val ML_MTU_PAYLOAD = 19
        const val CHUNK_GAP_MS = 60L
    }
}

/** When the device page may start [DeviceMetadataProbe]: never beside another BLE session or a history download. */
internal object DeviceMetadataStartPolicy {
    fun mayStart(bleSessionBusy: Boolean, historySyncActive: Boolean): Boolean = !bleSessionBusy && !historySyncActive
}
