package de.pulselibre

/**
 * Bounded, read-only synthetic-data download budget.
 * Offline policy: no BLE access, device mutation, or background scheduling.
 */
class CirqaSyntheticDownloadLimits {
    companion object {
        const val MAX_FILES = 256
        const val MAX_FILE_BYTES = 8L * 1024 * 1024
        const val MAX_TOTAL_BYTES = 64L * 1024 * 1024
        const val MAX_SESSION_MILLIS = 120_000L
    }

    var acceptedFiles = 0
        private set
    var acceptedBytes = 0L
        private set

    fun canAcceptFile(fileBytes: Long, elapsedMillis: Long): Boolean =
        elapsedMillis in 0..MAX_SESSION_MILLIS &&
            fileBytes in 0..MAX_FILE_BYTES &&
            acceptedFiles < MAX_FILES &&
            fileBytes <= MAX_TOTAL_BYTES - acceptedBytes

    fun acceptFile(fileBytes: Long, elapsedMillis: Long): Boolean {
        if (!canAcceptFile(fileBytes, elapsedMillis)) return false
        acceptedFiles++
        acceptedBytes += fileBytes
        return true
    }
}
