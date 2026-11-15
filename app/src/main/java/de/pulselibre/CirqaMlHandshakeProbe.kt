package de.pulselibre

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.UUID

/**
 * Explicit one-shot Garmin ML/GFDI handle registration test.
 * No GFDI application messages, file sync, health reads or persistent connection.
 */
class CirqaMlHandshakeProbe(private val context: Context, private val adapter: BluetoothAdapter) {
    private val main = Handler(Looper.getMainLooper())
    private val cccd = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private var active = false
    private var gatt: BluetoothGatt? = null
    private var callback: ((String) -> Unit)? = null
    private var tx: BluetoothGattCharacteristic? = null
    private var session: GarminMlHandshake.Session? = null
    private var requestedCloseAll = false
    private val notificationShapes = mutableListOf<String>()
    @Volatile private var phase = "Noch nicht gestartet"
    @Volatile private var notificationsReceived = 0
    @Volatile private var writeConfirmed = false
    private fun mark(value: String) { phase = value }

    @SuppressLint("MissingPermission")
    fun start(closeAll: Boolean, result: (String) -> Unit) {
        if (active) { result("ML-Test läuft bereits."); return }
        val device = try {
            adapter.bondedDevices.orEmpty().firstOrNull {
                it.name?.contains("CIRQA", ignoreCase = true) == true
            }
        } catch (_: SecurityException) { result("Bluetooth-Berechtigung fehlt."); return }
        if (device == null) { result("CIRQA nicht in Android-Kopplungen gefunden."); return }
        active = true
        notificationsReceived = 0
        notificationShapes.clear()
        writeConfirmed = false
        mark("GATT-Verbindung angefordert")
        callback = result
        requestedCloseAll = closeAll
        session = GarminMlHandshake.Session(closeAll)
        result("ML-Test: Verbinde und suche Garmin-Dienste …")
        try {
            gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(g: BluetoothGatt, status: Int, state: Int) {
                    if (!active) { safeClose(g); return }
                    if (status != BluetoothGatt.GATT_SUCCESS || state == BluetoothProfile.STATE_DISCONNECTED) {
                        finish("ML-Verbindung beendet (GATT $status, Zustand $state).", g)
                    } else if (state == BluetoothProfile.STATE_CONNECTED) {
                        mark("GATT verbunden; Dienstsuche läuft")
                        try { if (!g.discoverServices()) finish("GATT-Dienstsuche konnte nicht starten.", g) }
                        catch (_: SecurityException) { finish("Bluetooth-Berechtigung fehlt.", g) }
                    }
                }
                override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                    if (!active) return
                    if (status != BluetoothGatt.GATT_SUCCESS) { finish("Dienstsuche fehlgeschlagen: $status", g); return }
                    val service = g.services.orEmpty().firstOrNull {
                        it.uuid.toString().equals(GarminMlOffline.SERVICE_UUID, true)
                    }
                    val pair = GarminMlOffline.findCharacteristicPair(
                        service?.characteristics.orEmpty().map { it.uuid.toString() })
                    if (service == null || pair == null) {
                        finish("Garmin ML RX/TX-Paar im ML-Dienst nicht vorhanden.", g); return
                    }
                    val rx = service.getCharacteristic(UUID.fromString(pair.rx))
                    tx = service.getCharacteristic(UUID.fromString(pair.tx))
                    if (rx == null || tx == null || rx.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0 ||
                        tx!!.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
                        finish("Garmin ML Characteristics unterstützen Notify/Write nicht.", g); return
                    }
                    mark("ML RX/TX gefunden; CCCD wird aktiviert")
                    val descriptor = rx.getDescriptor(cccd)
                    if (descriptor == null) { finish("Garmin ML CCCD fehlt; kein Handshake gesendet.", g); return }
                    try {
                        if (!g.setCharacteristicNotification(rx, true)) {
                            finish("Garmin ML Notifications konnten nicht aktiviert werden.", g); return
                        }
                        val ok = if (Build.VERSION.SDK_INT >= 33)
                            g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS
                        else {
                            @Suppress("DEPRECATION")
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            @Suppress("DEPRECATION")
                            g.writeDescriptor(descriptor)
                        }
                        if (!ok) finish("CCCD-Schreibauftrag abgelehnt.", g)
                    } catch (_: SecurityException) { finish("Bluetooth-Berechtigung beim CCCD fehlt.", g) }
                }
                override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                    if (!active || descriptor.uuid != cccd) return
                    if (status != BluetoothGatt.GATT_SUCCESS) { finish("CCCD-Fehler: $status", g); return }
                    mark("CCCD bestätigt; erster ML-Befehl wird gesendet")
                    send(g, session!!.begin())
                }
                @Deprecated("Android compatibility callback")
                override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                    @Suppress("DEPRECATION")
                    receive(g, characteristic.value?.clone() ?: byteArrayOf())
                }
                override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                    receive(g, value)
                }
                override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                    if (!active) return
                    if (status != BluetoothGatt.GATT_SUCCESS) finish("ML-Schreibfehler: $status", g)
                    else { writeConfirmed = true; mark("ML-Schreiben bestätigt; warte auf Protokollantwort") }
                }
            }, BluetoothDevice.TRANSPORT_LE)
            main.postDelayed({ if (active) finish("ML-Handshake: Zeitlimit (20 Sekunden). Letzte Phase: $phase; Notifications: $notificationsReceived; GATT-Schreiben bestätigt: $writeConfirmed; Protokollzustand: ${session?.stage}; Antwortformen: ${notificationShapes.joinToString(" | ")}.", gatt) }, 20_000)
        } catch (_: SecurityException) { finish("Bluetooth-Berechtigung fehlt.", gatt) }
        catch (e: IllegalArgumentException) { finish("Verbindungsfehler: ${e.javaClass.simpleName}", gatt) }
    }

    @SuppressLint("MissingPermission")
    private fun send(g: BluetoothGatt, bytes: ByteArray) {
        val char = tx ?: run { finish("ML-TX fehlt.", g); return }
        val mode = if (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        mark(if (session?.stage == GarminMlHandshake.Stage.WAIT_CLOSE) "Sende CLOSE_ALL" else "Sende GFDI-Registrierung")
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
            if (!ok) finish("ML-Schreibauftrag abgelehnt.", g)
        } catch (_: SecurityException) { finish("Bluetooth-Berechtigung beim Schreiben fehlt.", g) }
    }

    private fun receive(g: BluetoothGatt, bytes: ByteArray) {
        if (!active) return
        notificationsReceived++
        if (notificationShapes.size < 12) {
            val first = bytes.firstOrNull()?.toInt()?.and(255)
            val second = bytes.getOrNull(1)?.toInt()?.and(255)
            val control = first == 0
            val parsed = GarminMlOffline.inspectRegisterResponse(bytes)
            notificationShapes.add("L=${bytes.size}, control=$control, type=${if (control) second else "-"}, reg=${if (parsed == null) "nein" else "service:${parsed.serviceId}/status:${parsed.status}/handle:${parsed.handle}"}")
        }
        mark("ML-Notification empfangen; Protokollantwort wird geprüft")
        val state = session ?: return
        val next = state.onNotification(bytes)
        if (next != null) { mark("CLOSE_ALL bestätigt; sende GFDI-Registrierung"); send(g, next); return }
        when (state.stage) {
            GarminMlHandshake.Stage.COMPLETE ->
                finish("Garmin ML: GFDI-Service erfolgreich registriert! ${if (requestedCloseAll) "CLOSE_ALL wurde zuvor bestätigt." else "Ohne CLOSE_ALL."} Verbindung beendet; keine Gesundheitsdaten gelesen.", g)
            GarminMlHandshake.Stage.FAILED ->
                finish("Garmin ML: GFDI-Registrierung vom Gerät abgelehnt.", g)
            else -> Unit
        }
    }

    @SuppressLint("MissingPermission")
    private fun safeClose(g: BluetoothGatt?) {
        if (g == null) return
        try { g.disconnect() } catch (_: Exception) {}
        try { g.close() } catch (_: Exception) {}
    }
    private fun finish(message: String, g: BluetoothGatt?) {
        main.post {
            if (!active) return@post
            active = false
            main.removeCallbacksAndMessages(null)
            safeClose(g)
            gatt = null
            callback?.invoke(message)
            callback = null
            tx = null
            session = null
        }
    }
    fun close() {
        active = false
        main.removeCallbacksAndMessages(null)
        safeClose(gatt)
        gatt = null
        callback = null
        session = null
    }
}
