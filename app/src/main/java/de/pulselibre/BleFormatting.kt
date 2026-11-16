package de.pulselibre

import android.bluetooth.BluetoothGattCharacteristic

fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it.toInt() and 0xff) }

fun describeProperties(flags: Int): String = buildList {
    if (flags and BluetoothGattCharacteristic.PROPERTY_READ != 0) add("READ")
    if (flags and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) add("WRITE")
    if (flags and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) add("NOTIFY")
    if (flags and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) add("INDICATE")
}.joinToString(", ").ifEmpty { "other" }
