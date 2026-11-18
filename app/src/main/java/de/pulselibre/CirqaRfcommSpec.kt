package de.pulselibre

import java.util.UUID

/**
 * Connect 5.31 Classic-Bluetooth file-transfer path (prepared, NOT active).
 *
 * Evidence: Garmin Connect `ph4.h` = UUID below, `a5.a()` opens
 * `listenUsingRfcommWithServiceRecord("Garmin SPP", uuid)` and
 * `GfdiDeviceCompat.open(inputStream, outputStream, ...)` runs the same GFDI
 * framing over the socket. An independent public report (Gadgetbridge #6879)
 * shows the identical 8192/32 placeholder behaviour over BLE and a real
 * directory over this RFCOMM service.
 *
 * No socket is opened and no request is sent unless a separately released gate
 * and explicit user confirmation are present.
 */
object CirqaRfcommSpec {
    const val SERVICE_NAME = "Garmin SPP"
    val SERVICE_UUID: UUID = UUID.fromString("DEAB91E4-670F-11E1-9ACE-30BC4824019B")

    /** Bounded acceptance window for an inbound device-initiated connection. */
    const val ACCEPT_TIMEOUT_MS = 20_000L
    /** Bounded overall directory-transfer budget over the socket. */
    const val TRANSFER_TIMEOUT_MS = 60_000L
    /** Upper bound for one decoded GFDI frame on this transport. */
    const val MAX_FRAME_BYTES = 64 * 1024
}

object CirqaRfcommGate {
    /** Deliberately false: preparing code is not permission to open a socket. */
    const val RELEASED_FOR_DEVICE_TEST = false
}
