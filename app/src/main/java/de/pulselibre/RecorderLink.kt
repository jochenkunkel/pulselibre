package de.pulselibre

/**
 * The part of a band connection the continuous recorder uses. Production: [CirqaMlHandshakeProbe].
 * Exists so the emulator can simulate a silent connection (stall watchdog, #529) without Bluetooth.
 */
interface RecorderLink {
    var onReading: ((Int, GarminRealtime.Reading) -> Unit)?
    fun startContinuousAllMetrics(result: (String) -> Unit)
    fun close()

    /**
     * Stops accepting work and invokes [onQuiescent] only after work already in
     * progress has finished. Simple links may close synchronously; asynchronous
     * implementations must override this and provide a real completion barrier.
     */
    fun closeWhenQuiescent(onQuiescent: () -> Unit) {
        close()
        onQuiescent()
    }
}
