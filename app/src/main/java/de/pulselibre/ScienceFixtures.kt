package de.pulselibre

import java.util.Random

/**
 * Synthetic series for development previews and tests ONLY. Every series carries [SeriesOrigin.SYNTHETIC] and
 * [SeriesTimeBasis.SYNTHETIC]; the UI must show them under a visible "synthetic" banner and never next to, or instead of,
 * real health data. Values are generated from a fixed seed and have no physiological meaning.
 */
object ScienceFixtures {
    const val LABEL = "SYNTHETISCH"
    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE

    private fun rng(seed: Long) = Random(seed)

    /** One point per minute with a device-off gap, heart-rate-0 sentinels and limited-confidence stretches. */
    fun heartRate(startMs: Long, hours: Int = 24): SeriesData {
        val r = rng(1)
        val points = ArrayList<SeriesPoint>()
        for (m in 0 until hours * 60) {
            val t = startMs + m * MINUTE
            val hourOfSeries = m / 60
            if (hourOfSeries % 24 in 2..3) continue // device not worn: no points at all, a gap
            val value = 62 + 10 * Math.sin(m / 90.0) + r.nextGaussian() * 2
            points += when {
                m % 97 == 0 -> SeriesPoint(t, null, SeriesQuality.INVALID, rawCode = 0)
                m % 211 == 0 -> SeriesPoint(t, Math.round(value).toDouble(), SeriesQuality.LIMITED)
                else -> SeriesPoint(t, Math.round(value).toDouble())
            }
        }
        return SeriesData("heart_rate", "bpm", SeriesOrigin.SYNTHETIC, SeriesTimeBasis.SYNTHETIC, startMs, startMs + hours * HOUR, MINUTE, points)
    }

    /** Respiration with the Garmin status codes (-100 off wrist, -200 large motion, -300 invalid): status points, no value. */
    fun respiration(startMs: Long, hours: Int = 24): SeriesData {
        val r = rng(2)
        val points = ArrayList<SeriesPoint>()
        for (m in 0 until hours * 60 step 5) {
            val t = startMs + m * MINUTE
            points += when (m / 5 % 17) {
                3 -> SeriesPoint(t, null, SeriesQuality.STATUS, rawCode = -100)
                7 -> SeriesPoint(t, null, SeriesQuality.STATUS, rawCode = -200)
                11 -> SeriesPoint(t, null, SeriesQuality.STATUS, rawCode = -300)
                else -> SeriesPoint(t, Math.round(14 + r.nextGaussian()).toDouble())
            }
        }
        return SeriesData("breaths_per_min", "breaths/min", SeriesOrigin.SYNTHETIC, SeriesTimeBasis.SYNTHETIC, startMs, startMs + hours * HOUR, 5 * MINUTE, points)
    }

    /** SpO2: numerically valid values are only UNVERIFIED (no invented quality threshold). */
    fun spo2(startMs: Long, hours: Int = 24): SeriesData {
        val r = rng(3)
        val points = (0 until hours * 60 step 10).map { m ->
            SeriesPoint(startMs + m * MINUTE, Math.round(96 + r.nextGaussian()).coerceIn(90, 100).toDouble(), SeriesQuality.UNVERIFIED)
        }
        return SeriesData("spo2_percent", "%", SeriesOrigin.SYNTHETIC, SeriesTimeBasis.SYNTHETIC, startMs, startMs + hours * HOUR, 10 * MINUTE, points)
    }

    /** Stress: negative raw values are not shown as stress values (meaning unestablished). */
    fun stress(startMs: Long, hours: Int = 24): SeriesData {
        val r = rng(4)
        val points = (0 until hours * 60 step 15).map { m ->
            val t = startMs + m * MINUTE
            if (m / 15 % 9 == 4) SeriesPoint(t, null, SeriesQuality.INVALID, rawCode = -1)
            else SeriesPoint(t, Math.round(30 + 15 * Math.abs(r.nextGaussian())).coerceIn(0, 100).toDouble())
        }
        return SeriesData("stress", "device scale", SeriesOrigin.SYNTHETIC, SeriesTimeBasis.SYNTHETIC, startMs, startMs + hours * HOUR, 15 * MINUTE, points)
    }

    /** Steps per 15 minutes with a missing stretch (no data, not "zero steps"). */
    fun activity(startMs: Long, hours: Int = 24): SeriesData {
        val r = rng(5)
        val points = ArrayList<SeriesPoint>()
        for (m in 0 until hours * 60 step 15) {
            val t = startMs + m * MINUTE
            val h = m / 60 % 24
            if (h in 13..15) continue
            val steps = if (h in 0..6) 0.0 else Math.round(Math.abs(r.nextGaussian()) * 120).toDouble()
            points += SeriesPoint(t, steps)
        }
        return SeriesData("steps_interval", "steps", SeriesOrigin.SYNTHETIC, SeriesTimeBasis.SYNTHETIC, startMs, startMs + hours * HOUR, 15 * MINUTE, points)
    }

    /** One night: light/deep/REM cycles, one awake interruption, one stretch without data. */
    fun sleep(startMs: Long): List<SleepInterval> {
        var t = startMs
        fun add(minutes: Int, phase: SleepPhase): SleepInterval { val i = SleepInterval(t, t + minutes * MINUTE, phase); t = i.endMs; return i }
        val out = ArrayList<SleepInterval>()
        out += add(20, SleepPhase.LIGHT); out += add(45, SleepPhase.DEEP); out += add(30, SleepPhase.LIGHT)
        out += add(15, SleepPhase.AWAKE) // interruption
        out += add(40, SleepPhase.REM); out += add(50, SleepPhase.LIGHT)
        t += 25 * MINUTE // no data (not awake)
        out += add(35, SleepPhase.DEEP); out += add(30, SleepPhase.REM); out += add(10, SleepPhase.AWAKE)
        return out
    }
}
