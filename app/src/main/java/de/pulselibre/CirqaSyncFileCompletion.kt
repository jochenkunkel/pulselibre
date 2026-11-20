package de.pulselibre

/** Classifies the redacted result emitted by the bounded MLR file validator. */
internal object CirqaSyncFileCompletion {
    fun isSuccessful(validation: String): Boolean {
        val rawStored = validation.contains("privateRaw=CREATED") ||
            validation.contains("privateRaw=ALREADY_PRESENT")
        if (rawStored) {
            // Retaining malformed bytes is useful for offline diagnosis, but a
            // declared FIT that failed envelope/CRC verification is not a
            // successfully processed history file.
            val invalidFit = validation.contains("fitHeader=false") ||
                Regex("(?:^|[;:])type=FIT_TYPE_[A-Z0-9_]+").containsMatchIn(validation)
            return validation.contains("exactSize=true") && !invalidFit
        }

        val fitStored = validation.contains("privateFit=CREATED") ||
            validation.contains("privateFit=ALREADY_PRESENT")
        return fitStored && validation.contains(";sqlite=ok:")
    }
}
