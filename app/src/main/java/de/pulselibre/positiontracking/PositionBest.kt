package de.pulselibre.positiontracking

/** Picks the more precise of two fixes; a fix with known accuracy beats one without; ties keep the newer one. */
object PositionBest {
    fun pick(current: PositionPoint?, candidate: PositionPoint): PositionPoint {
        if (current == null) return candidate
        val a = current.accuracyMeters
        val b = candidate.accuracyMeters
        return when {
            b == null && a != null -> current
            a == null && b != null -> candidate
            a != null && b != null && b < a -> candidate
            a != null && b != null && b > a -> current
            else -> if (candidate.timeMs >= current.timeMs) candidate else current
        }
    }
}
