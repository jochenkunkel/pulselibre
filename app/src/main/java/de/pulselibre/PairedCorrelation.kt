package de.pulselibre

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ceil
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
        val note: String,
        /** Two-sided p for rho = 0 (Fisher z with the same effective n as the interval); null when not computable. */
        val pValue: Double? = null
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
        val twoSided = 2.0 * (1.0 - normalCdf(abs(z) / se))
        return Result(n, rho, r, effective, lo, hi, lagDays, verdict,
            "Spearman ρ, 95-%-Intervall mit effektivem n≈${"%.0f".format(java.util.Locale.GERMANY, effective)}; keine Aussage über Ursache und Wirkung.",
            twoSided.coerceIn(0.0, 1.0))
    }

    /**
     * Robustness check (#45): how much does rho depend on single days?
     * [looMin]/[looMax]: Spearman rho when each single paired day is left out in turn.
     * [trimmedRho]: rho without the [trimmedCount] days (5 %, at least 1) whose rank positions are most extreme in x and y.
     * [stable]: same sign everywhere and no deviation from the full-sample rho of 0.1 or more.
     */
    data class Sensitivity(
        val baseRho: Double, val looMin: Double, val looMax: Double,
        val trimmedRho: Double, val trimmedCount: Int, val stable: Boolean
    ) {
        fun text(nf: java.text.NumberFormat): String =
            "Robustheit: ohne einzelne Tage ρ ${nf.format(looMin)} bis ${nf.format(looMax)}; ohne die $trimmedCount auffälligsten Tage " +
                "ρ ${nf.format(trimmedRho)} – " + if (stable) "stabil." else "nicht stabil (einzelne Tage beeinflussen das Ergebnis)."
    }

    fun sensitivity(x: Map<LocalDate, Double>, y: Map<LocalDate, Double>, lagDays: Int): Sensitivity? {
        val p = pair(x, y, lagDays)
        val n = p.size
        if (n < MIN_PAIRS) return null
        val xs = p.map { it.second }; val ys = p.map { it.third }
        if (xs.distinct().size < 2 || ys.distinct().size < 2) return null
        fun rho(ix: List<Int>): Double? {
            val a = ix.map { xs[it] }; val b = ix.map { ys[it] }
            if (a.distinct().size < 2 || b.distinct().size < 2) return null
            return pearson(ranks(a), ranks(b))
        }
        val all = xs.indices.toList()
        val base = rho(all) ?: return null
        val loo = all.mapNotNull { drop -> rho(all.filter { it != drop }) }
        if (loo.isEmpty()) return null
        // Extremeness by rank distance from the middle in both series.
        val rx = ranks(xs); val ry = ranks(ys); val mid = (n + 1) / 2.0
        val trimCount = ceil(n * 0.05).toInt().coerceAtLeast(1)
        val drop = all.sortedByDescending { abs(rx[it] - mid) + abs(ry[it] - mid) }.take(trimCount).toSet()
        val trimmed = rho(all.filter { it !in drop }) ?: return null
        val sign = Math.signum(base)
        val sameSign = sign != 0.0 && Math.signum(trimmed) == sign && loo.all { Math.signum(it) == sign }
        val stable = sameSign && abs(trimmed - base) < 0.1 && loo.all { abs(it - base) < 0.1 }
        return Sensitivity(base, loo.min(), loo.max(), trimmed, trimCount, stable)
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

    /** Standard normal CDF (Abramowitz & Stegun 7.1.26, absolute error < 1.5e-7). */
    fun normalCdf(x: Double): Double {
        val t = 1.0 / (1.0 + 0.3275911 * abs(x) / sqrt(2.0))
        val poly = t * (0.254829592 + t * (-0.284496736 + t * (1.421413741 + t * (-1.453152027 + t * 1.061405429))))
        val erf = 1.0 - poly * kotlin.math.exp(-x * x / 2.0)
        return if (x >= 0) 0.5 * (1.0 + erf) else 0.5 * (1.0 - erf)
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
