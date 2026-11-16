package de.pulselibre

/** An address equality hint only, not proof of device identity or protocol support. */
object CirqaAddressMatch {
    fun matches(bondAddress: String?, advertisementAddress: String?): Boolean {
        if (bondAddress.isNullOrBlank() || advertisementAddress.isNullOrBlank()) return false
        return bondAddress.trim().equals(advertisementAddress.trim(), ignoreCase = true)
    }
}
