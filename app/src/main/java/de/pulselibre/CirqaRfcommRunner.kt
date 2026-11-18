package de.pulselibre

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Bounded, read-only Classic-RFCOMM directory run. One attempt per persistent
 * gate, no retry, no device mutation. Requires [CirqaRfcommGate] activation and
 * an explicit expected (paired) Bluetooth address.
 */
class CirqaRfcommRunner {
    private val transport = CirqaRfcommTransport()
    private val main = Handler(Looper.getMainLooper())
    private val stopped = AtomicBoolean(false)
    @Volatile private var connected = false
    @Volatile private var sdpSeen = ""
    private var sdpReceiver: BroadcastReceiver? = null

    fun stop() { stopped.set(true); transport.cancel() }

    fun start(context: Context, expectedAddress: String, result: (String) -> Unit) {
        if (!CirqaRfcommGate.RELEASED_FOR_DEVICE_TEST) {
            result("RFCOMM gesperrt: keine Freigabe.")
            return
        }
        stopped.set(false)
        connected = false
        sdpSeen = ""
        registerSdpReceiver(context, expectedAddress, result)
        Thread {
            var report = "RFCOMM: kein Ergebnis."
            val acceptCancel = Runnable { transport.timeout() }
            main.postDelayed(acceptCancel, CirqaRfcommSpec.ACCEPT_TIMEOUT_MS)
            try {
                report = when (val connected = transport.accept(expectedAddress)) {
                    CirqaRfcommTransport.Result.GateBlocked -> "RFCOMM: Gate gesperrt."
                    is CirqaRfcommTransport.Result.Failed ->
                        "RFCOMM: Verbindung abgelehnt (${connected.reason})."
                    is CirqaRfcommTransport.Result.Connected -> {
                        this@CirqaRfcommRunner.connected = true
                        runDirectory(connected, result)
                    }
                }
            } catch (_: IOException) {
                report = "RFCOMM: Socket-I/O beendet."
            } catch (_: SecurityException) {
                report = "RFCOMM: Berechtigung fehlt."
            } finally {
                main.removeCallbacks(acceptCancel)
                transport.cancel()
                val sdpSuffix = if (sdpSeen.isEmpty()) "" else " sdp=[${sdpSeen.take(160)}]"
                val finalReport = report + sdpSuffix
                main.post {
                    sdpReceiver?.let { runCatching { context.unregisterReceiver(it) } }
                    sdpReceiver = null
                    result(finalReport)
                }
            }
        }.apply { isDaemon = true; name = "cirqa-rfcomm" }.start()
        // Give the acceptor thread time to register the SPP server socket, then
        // ask the paired device (read-only SDP) to initiate the RFCOMM link.
        main.postDelayed({ requestSdp(expectedAddress, result) }, 1_000L)
    }

    private fun registerSdpReceiver(context: Context, address: String, progress: (String) -> Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device?.address?.equals(address, ignoreCase = true) != true) return
                val uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)
                    ?.mapNotNull { (it as? ParcelUuid)?.uuid?.toString() } ?: emptyList()
                sdpSeen = uuids.joinToString(",")
                val offersSpp = uuids.any {
                    it.equals(CirqaRfcommSpec.SERVICE_UUID.toString(), ignoreCase = true)
                }
                main.post {
                    progress("RFCOMM: SDP ${uuids.size} UUIDs; SPP angeboten=$offersSpp")
                }
            }
        }
        sdpReceiver = receiver
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(BluetoothDevice.ACTION_UUID),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    @SuppressLint("MissingPermission")
    private fun requestSdp(address: String, progress: (String) -> Unit) {
        if (stopped.get() || connected) return
        try {
            val device = BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(address)
            if (device == null) {
                main.post { progress("RFCOMM: kein Bluetooth-Adapter für SDP.") }
                return
            }
            val started = device.fetchUuidsWithSdp()
            main.post {
                progress("RFCOMM: SDP-Query ${if (started) "gestartet" else "abgelehnt"}; warte auf Verbindung.")
            }
        } catch (_: SecurityException) {
            main.post { progress("RFCOMM: SDP-Query ohne Berechtigung.") }
        }
    }

    private fun runDirectory(
        connected: CirqaRfcommTransport.Result.Connected,
        progress: (String) -> Unit
    ): String {
        val session = CirqaRfcommDirectorySession()
        // Read-only size request for directory index 0; no handle byte on RFCOMM.
        connected.output.write(
            GarminFileListRequest.cobs(CirqaLegacy5002RequestCandidate.gfdiFrame(0, false))
        )
        connected.output.flush()
        main.post { progress("RFCOMM: verbunden, 5002 gesendet; warte auf Größe und Chunks.") }

        val transferTimeout = Runnable { stop() }
        main.postDelayed(transferTimeout, CirqaRfcommSpec.TRANSFER_TIMEOUT_MS)
        val buffer = ByteArray(4096)
        try {
            while (!stopped.get() && session.phase != CirqaDirectoryReadV2Session.Phase.COMPLETED) {
                val read = connected.input.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                for (ack in session.acceptStream(buffer.copyOfRange(0, read))) {
                    connected.output.write(GarminFileListRequest.cobs(ack))
                }
                connected.output.flush()
            }
        } finally {
            main.removeCallbacks(transferTimeout)
        }
        val detail = if (session.sizeError.isNotEmpty()) " sizeError=${session.sizeError}" else ""
        return "RFCOMM-Verzeichnis: ${session.summary()}; $detail" +
            " acks=${session.ackFramesBuilt}; 5043=${session.controlFrames}"
    }
}
