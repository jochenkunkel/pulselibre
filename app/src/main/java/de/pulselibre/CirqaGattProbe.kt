package de.pulselibre

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * User-triggered GATT discovery only: connect -> discover services -> disconnect.
 * Does not read, write, subscribe, pair, unpair, or send CLOSE_ALL.
 * The Garmin ML handshake is a separate future opt-in step.
 */
class CirqaGattProbe(private val context: Context, private val adapter: BluetoothAdapter) {
    private val handler = Handler(Looper.getMainLooper())
    private var gatt: BluetoothGatt? = null
    private var active = false
    private var onResult: ((String) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun start(callback: (String) -> Unit) {
        if (active) { callback("GATT-Test läuft bereits."); return }
        val device: BluetoothDevice = try {
            adapter.bondedDevices.orEmpty().firstOrNull {
                it.name?.contains("CIRQA", ignoreCase = true) == true
            } ?: run { callback("Kein gekoppeltes CIRQA gefunden. Keine Verbindung aufgebaut."); return }
        } catch (_: SecurityException) {
            callback("Bluetooth-Berechtigung fehlt."); return
        }
        active = true
        onResult = callback
        callback("CIRQA: GATT-Verbindung wird angefragt (nur Dienstsuche).")
        try {
            gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                    if (!active) { safeClose(g); return }
                    if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                        finish("GATT beendet: Status $status, Zustand $newState", g)
                    } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                        try {
                            if (!g.discoverServices()) finish("Dienstsuche konnte nicht gestartet werden.", g)
                        } catch (_: SecurityException) { finish("Bluetooth-Berechtigung während Dienstsuche entzogen.", g) }
                    }
                }
                override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                    if (!active) { safeClose(g); return }
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        finish("GATT-Dienstsuche fehlgeschlagen: $status", g)
                        return
                    }
                    val services: List<BluetoothGattService> = g.services.orEmpty()
                    val chars = services.flatMap { service: BluetoothGattService ->
                        service.characteristics.map { char: BluetoothGattCharacteristic -> char.uuid.toString() }
                    }
                    val pair = GarminMlOffline.findCharacteristicPair(chars)
                    val mlService = services.any { it.uuid.toString().equals(GarminMlOffline.SERVICE_UUID, true) }
                    val result = buildString {
                        appendLine("GATT-Dienstsuche abgeschlossen (nur Metadaten)")
                        appendLine("Dienste: ${services.size}, Characteristics: ${chars.size}")
                        appendLine("Garmin ML Service: ${if (mlService) "gefunden" else "nicht gefunden"}")
                        appendLine("Garmin RX/TX-Paar: ${if (pair != null) "gefunden" else "nicht gefunden"}")
                        append("Keine GATT-Lese-/Schreibbefehle oder Abonnements.")
                    }
                    finish(result, g)
                }
            }, BluetoothDevice.TRANSPORT_LE)
            handler.postDelayed({ if (active) finish("GATT-Zeitlimit nach 15 Sekunden.", gatt) }, 15_000)
        } catch (_: SecurityException) { finish("Bluetooth-Berechtigung fehlt.", gatt) }
        catch (e: IllegalArgumentException) { finish("GATT-Verbindung nicht möglich: ${e.javaClass.simpleName}", gatt) }
    }

    @SuppressLint("MissingPermission")
    private fun safeClose(g: BluetoothGatt?) {
        if (g == null) return
        try { g.disconnect() } catch (_: Exception) {}
        try { g.close() } catch (_: Exception) {}
    }

    private fun finish(message: String, g: BluetoothGatt?) {
        handler.post {
            if (!active) { safeClose(g); return@post }
            active = false
            handler.removeCallbacksAndMessages(null)
            safeClose(g)
            gatt = null
            onResult?.invoke(message)
            onResult = null
        }
    }

    fun close() {
        active = false
        handler.removeCallbacksAndMessages(null)
        safeClose(gatt)
        gatt = null
        onResult = null
    }
}
