package de.pulselibre

/** Human-readable subset of Android GATT connection statuses seen in diagnostics. */
object CirqaGattConnectionStatus {
    // BluetoothGatt.GATT_CONNECTION_TIMEOUT (0x93), added as a named API constant in API 35.
    const val CONNECTION_TIMEOUT = 147

    fun description(status: Int): String? = when (status) {
        0 -> "GATT_SUCCESS"
        CONNECTION_TIMEOUT ->
            "GATT_CONNECTION_TIMEOUT (remote may be out of range or not advertising as connectable)"
        else -> null
    }
}
