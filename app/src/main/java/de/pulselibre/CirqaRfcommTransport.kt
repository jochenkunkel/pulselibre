package de.pulselibre

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Prepared Classic-RFCOMM acceptor for the Connect/SPP file-transfer service.
 *
 * NOT active: [CirqaRfcommGate.RELEASED_FOR_DEVICE_TEST] is false and no method
 * opens a socket until it is changed together with an explicit user release.
 * The caller must run [accept] on a background thread and may call [cancel]
 * to unblock it. Only an already-bonded, explicitly expected address is accepted.
 */
class CirqaRfcommTransport {
    sealed class Result {
        data class Connected(val input: InputStream, val output: OutputStream, val address: String) : Result()
        object GateBlocked : Result()
        data class Failed(val reason: String) : Result()
    }

    @Volatile private var serverSocket: BluetoothServerSocket? = null
    @Volatile private var socket: BluetoothSocket? = null

    @SuppressLint("MissingPermission")
    fun accept(expectedAddress: String?): Result {
        if (!CirqaRfcommGate.RELEASED_FOR_DEVICE_TEST) return Result.GateBlocked
        if (expectedAddress.isNullOrBlank()) return Result.Failed("no-expected-address")
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return Result.Failed("no-adapter")
        if (!adapter.isEnabled) return Result.Failed("adapter-off")
        return try {
            val server = adapter.listenUsingRfcommWithServiceRecord(
                CirqaRfcommSpec.SERVICE_NAME, CirqaRfcommSpec.SERVICE_UUID
            )
            serverSocket = server
            val accepted = server.accept()
            socket = accepted
            val remote = accepted.remoteDevice?.address
            if (remote == null || !remote.equals(expectedAddress, ignoreCase = true)) {
                cancel()
                return Result.Failed("unexpected-device")
            }
            Result.Connected(accepted.inputStream, accepted.outputStream, remote)
        } catch (_: SecurityException) {
            Result.Failed("permission-denied")
        } catch (_: IOException) {
            Result.Failed("io-error")
        }
    }

    /** Unblocks a pending [accept] and closes all sockets. Idempotent. */
    @Synchronized fun cancel() {
        try { serverSocket?.close() } catch (_: IOException) { }
        try { socket?.close() } catch (_: IOException) { }
        serverSocket = null
        socket = null
    }

    val summary: String
        get() = "CirqaRfcomm: gate=${CirqaRfcommGate.RELEASED_FOR_DEVICE_TEST}; " +
            "uuid=${CirqaRfcommSpec.SERVICE_UUID}; connected=${socket != null}"
}
