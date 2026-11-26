package de.pulselibre

/**
 * Progress of a long backup operation (owner feedback 25.11.2026: "bis auf das warten war alles ok"). With a large
 * database, creating, checking and restoring each take about a minute; the screen now says which step runs instead of
 * a static "läuft …". Called from the worker thread; the UI posts it to the main thread.
 */
fun interface BackupProgress {
    enum class Phase { SNAPSHOT, WRITE, COPY, EXTRACT, VALIDATE, REFERENCES, RESTORE, SETTINGS }

    /** [done] of [total] steps of [phase]; total 0 means "running, size unknown". */
    fun update(phase: Phase, done: Int, total: Int)

    companion object {
        val NONE = BackupProgress { _, _, _ -> }
    }
}

/** Wording and overall percentage of a progress update; pure so it can be unit-tested. */
internal object BackupProgressText {
    /** Rough share of the total time per phase, measured with the 153 MiB test store (backup 48 s, check 74 s). */
    private val backupWeights = mapOf(BackupProgress.Phase.SNAPSHOT to 0..15, BackupProgress.Phase.WRITE to 15..90,
        BackupProgress.Phase.COPY to 90..100)
    private val checkWeights = mapOf(BackupProgress.Phase.EXTRACT to 0..30, BackupProgress.Phase.VALIDATE to 30..85,
        BackupProgress.Phase.REFERENCES to 85..100)

    /** Overall percent 0–100, or null when the phase has no measurable share (restore runs in one transaction). */
    fun percent(phase: BackupProgress.Phase, done: Int, total: Int): Int? {
        val range = backupWeights[phase] ?: checkWeights[phase] ?: return null
        val share = if (total <= 0) 0.0 else (done.coerceIn(0, total).toDouble() / total)
        return (range.first + (range.last - range.first) * share).toInt().coerceIn(0, 100)
    }

    fun text(strings: Strings, phase: BackupProgress.Phase, done: Int, total: Int): String {
        val step = when (phase) {
            BackupProgress.Phase.SNAPSHOT -> strings.get(R.string.ex_progress_snapshot)
            BackupProgress.Phase.WRITE -> strings.get(R.string.ex_progress_write, done, total)
            BackupProgress.Phase.COPY -> strings.get(R.string.ex_progress_copy)
            BackupProgress.Phase.EXTRACT -> strings.get(R.string.ex_progress_extract)
            BackupProgress.Phase.VALIDATE -> strings.get(R.string.ex_progress_validate, done, total)
            BackupProgress.Phase.REFERENCES -> strings.get(R.string.ex_progress_references)
            BackupProgress.Phase.RESTORE -> strings.get(R.string.ex_progress_restore)
            BackupProgress.Phase.SETTINGS -> strings.get(R.string.ex_progress_settings)
        }
        return percent(phase, done, total)?.let { "$step · $it %" } ?: step
    }
}
