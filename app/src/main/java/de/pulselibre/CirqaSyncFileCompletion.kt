package de.pulselibre

/** Classifies the redacted result emitted by the bounded MLR file validator. */
internal object CirqaSyncFileCompletion {
    fun isSuccessful(validation: String): Boolean {
        val rawStored = validation.contains("privateRaw=CREATED") ||
            validation.contains("privateRaw=ALREADY_PRESENT")
        if (rawStored) return validation.contains("exactSize=true")

        val fitStored = validation.contains("privateFit=CREATED") ||
            validation.contains("privateFit=ALREADY_PRESENT")
        return fitStored && validation.contains(";sqlite=ok:")
    }
}
