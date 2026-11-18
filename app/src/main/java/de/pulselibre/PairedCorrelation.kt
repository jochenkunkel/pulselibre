package de.pulselibre

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.atanh
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Pre-specified, explorative association of two daily series (#44/#45). Pure logic, no UI.
 * Spearman rank correlation, 95 % Fisher-z interval with an effective sample size reduced for
 * lag-1 autocorrelation (Bretherton et al. 1999), and a hard minimum number of paired days.
 * Correlation is not causation; results are never reported without n and interval.
 */
internal object PairedCorrelation {
    const val MIN_PAIRS = 30
    /** Smallest |rho| treated as a (weak) association worth reporting. */
    const val MIN_EFFECT = 0.1

    enum class Verdict(val label: String) {
        NOT_COMPUTABLE("nicht berechenbar"),
        NO_RELIABLE_ASSOCIATION("kein belastbarer Zusammenhang"),
        EXPLORATORY_ASSOCIATION("explorativer Zusammenhang")
    }

    data class Result(
        val pairs: Int,
        val rho: Double?,
        val pearson: Double?,
        val effectiveN: Double?,
        val ciLow: Double?,
        val ciHigh: Double?,
        val lagDays: Int,
        val verdict: Verdict,
        val note: String
    )

    /** Pairs x(day) with y(day + lagDays); only days where both exist. Lag 1 = effect on the following day. */
    fun pair(x: Map<LocalDate, Double>, y: Map<LocalDate, Double>, lagDays: Int): List<Triple<LocalDate, Double, Double>> =
        x.keys.sorted().mapNotNull { d ->
            val xv = x[d]?.takeIf(Double::isFinite) ?: return@mapNotNull null
            val yv = y[d.plusDays(lagDays.toLong())]?.takeIf(Double::isFinite) ?: return@mapNotNull null
            Triple(d, xv, yv)
        }

    fun analyze(x: Map<LocalDate, Double>, y: Map<LocalDate, Double>, lagDays: Int): Result {
        val p = pair(x, y, lagDays)
        val n = p.size
        if (n < MIN_PAIRS) return Result(n, null, null, null, null, null, lagDays, Verdict.NOT_COMPUTABLE,
            "Nur $n gepaarte Tage, mindestens $MIN_PAIRS nötig.")
        val xs = p.map { it.second }
        val ys = p.map { it.third }
        if (xs.distinct().size < 2 || ys.distinct().size < 2)
            return Result(n, null, null, null, null, null, lagDays, Verdict.NOT_COMPUTABLE, "Eine Größe ist konstant; keine Korrelation definierbar.")
        val rho = pearson(ranks(xs), ranks(ys))
        val r = pearson(xs, ys)
        // Autocorrelation of the paired series in date order (p is sorted by day).
        val effective = (n * (1 - autocorr(xs) * autocorr(ys)) / (1 + autocorr(xs) * autocorr(ys))).coerceIn(0.0, n.toDouble())
        if (effective <= 3.0 + 1e-9) return Result(n, rho, r, effective, null, null, lagDays, Verdict.NOT_COMPUTABLE,
            "Wegen zeitlicher Abhängigkeit zu wenige unabhängige Beobachtungen.")
        val se = 1.0 / sqrt(effective - 3.0)
        val z = atanh(rho.coerceIn(-0.999999, 0.999999))
        val lo = tanh(z - 1.96 * se)
        val hi = tanh(z + 1.96 * se)
        val excludesZero = lo > 0 || hi < 0
        val verdict = if (excludesZero && abs(rho) >= MIN_EFFECT) Verdict.EXPLORATORY_ASSOCIATION else Verdict.NO_RELIABLE_ASSOCIATION
        return Result(n, rho, r, effective, lo, hi, lagDays, verdict,
            "Spearman ρ, 95-%-Intervall mit effektivem n≈${"%.0f".format(java.util.Locale.GERMANY, effective)}; keine Aussage über Ursache und Wirkung.")
    }

    /** Benjamini–Hochberg adjusted p-values (same order as input), for multiple pre-specified hypotheses. */
    fun benjaminiHochberg(p: List<Double>): List<Double> {
        require(p.all { it in 0.0..1.0 })
        val m = p.size
        val order = p.indices.sortedBy { p[it] }
        val adj = DoubleArray(m)
        var running = 1.0
        for (rank in m downTo 1) {
            val i = order[rank - 1]
            running = minOf(running, p[i] * m / rank)
            adj[i] = running
        }
        return adj.toList()
    }

    fun ranks(v: List<Double>): List<Double> {
        val idx = v.indices.sortedBy { v[it] }
        val out = DoubleArray(v.size)
        var i = 0
        while (i < idx.size) {
            var j = i
            while (j + 1 < idx.size && v[idx[j + 1]] == v[idx[i]]) j++
            val avg = (i + j) / 2.0 + 1
            for (k in i..j) out[idx[k]] = avg
            i = j + 1
        }
        return out.toList()
    }

    fun pearson(a: List<Double>, b: List<Double>): Double {
        val ma = a.average(); val mb = b.average()
        var sab = 0.0; var saa = 0.0; var sbb = 0.0
        for (i in a.indices) { val da = a[i] - ma; val db = b[i] - mb; sab += da * db; saa += da * da; sbb += db * db }
        return if (saa == 0.0 || sbb == 0.0) 0.0 else sab / sqrt(saa * sbb)
    }

    /** Lag-1 autocorrelation, clamped to [0, 0.99]: only positive dependence shrinks the effective n. */
    private fun autocorr(v: List<Double>): Double {
        if (v.size < 3) return 0.0
        val a = v.dropLast(1); val b = v.drop(1)
        return pearson(a, b).coerceIn(0.0, 0.99)
    }
}
