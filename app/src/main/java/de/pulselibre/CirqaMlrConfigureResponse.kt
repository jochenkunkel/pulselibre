package de.pulselibre

/** The first data callback Connect consumes as an MLR configure response. */
object CirqaMlrConfigureResponse {
    fun isSuccessful(response: ByteArray): Boolean =
        response.size >= 3 &&
            response[0].toInt() and 0xff == 0 &&
            response[1].toInt() and 0xff == 0 &&
            response[2].toInt() and 0xff == 0
}
