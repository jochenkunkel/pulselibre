package de.pulselibre

import android.content.Context
import android.database.sqlite.SQLiteDatabase

/**
 * Testlabor part C (#528): bounded communication trace.
 *
 * The recorder opens no BLE session, scans nothing and writes nothing to the band. "Start" only marks a time window
 * of at most [MAX_WINDOW_MS]; the trace shows the raw notifications that the running sync already archives losslessly
 * (#600, [CirqaRawNotificationArchive], only while a sync is accepted). Full bytes are shown only after a separate
 * opt-in that every new start resets; by default only time offset, size and channel byte are listed.
 */
internal object CommunicationTrace {
    const val MAX_WINDOW_MS = 15L * 60_000L
    const val MAX_ENTRIES = 64
    private const val PREFS = "communication_trace"

    sealed class State {
        object Idle : State()
        data class Recording(val startMs: Long, val endMs: Long) : State()
        data class Finished(val startMs: Long, val endMs: Long) : State()
    }

    /** Pure window logic; [now] decides whether a started window has expired by itself. */
    fun state(startMs: Long?, stopMs: Long?, now: Long): State {
        if (startMs == null) return State.Idle
        val limit = startMs + MAX_WINDOW_MS
        val end = minOf(stopMs ?: limit, limit)
        return if (stopMs == null && now < limit) State.Recording(startMs, limit) else State.Finished(startMs, end)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun current(context: Context, now: Long = System.currentTimeMillis()): State = prefs(context).let { p ->
        state(p.getLong("start", -1L).takeIf { it >= 0 }, p.getLong("stop", -1L).takeIf { it >= 0 }, now)
    }

    fun start(context: Context, now: Long = System.currentTimeMillis()) {
        prefs(context).edit().putLong("start", now).remove("stop").putBoolean("payloads", false).apply()
    }

    fun stop(context: Context, now: Long = System.currentTimeMillis()) {
        if (current(context, now) is State.Recording) prefs(context).edit().putLong("stop", now).apply()
    }

    fun payloadsAllowed(context: Context): Boolean = prefs(context).getBoolean("payloads", false)
    fun allowPayloads(context: Context, allowed: Boolean) { prefs(context).edit().putBoolean("payloads", allowed).apply() }

    data class Entry(val atMs: Long, val bytes: Int, val channel: Int, val payload: ByteArray?)
    data class Trace(val total: Long, val totalBytes: Long, val entries: List<Entry>)

    /** Reads the archived notifications inside [from, to]; payloads are only selected with [withPayloads]. */
    fun read(db: SQLiteDatabase, from: Long, to: Long, withPayloads: Boolean): Trace {
        val args = arrayOf(from.toString(), to.toString())
        val window = "r.type_label='notification' AND r.received_at_ms BETWEEN ? AND ?"
        val (total, totalBytes) = db.rawQuery(
            "SELECT COUNT(*), COALESCE(SUM(f.file_bytes),0) FROM downloaded_file_references r " +
                "JOIN downloaded_files f ON f.content_sha256=r.content_sha256 WHERE $window", args
        ).use { if (it.moveToFirst()) it.getLong(0) to it.getLong(1) else 0L to 0L }
        val bytesColumn = if (withPayloads) "substr(f.payload,1,${BleSafetyPolicy.maximumDiagnosticBytes})" else "substr(f.payload,1,1)"
        val entries = db.rawQuery(
            "SELECT r.received_at_ms, f.file_bytes, $bytesColumn FROM downloaded_file_references r " +
                "JOIN downloaded_files f ON f.content_sha256=r.content_sha256 WHERE $window " +
                "ORDER BY r.received_at_ms, r.content_sha256 LIMIT $MAX_ENTRIES", args
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val head = c.getBlob(2)
                    add(Entry(c.getLong(0), c.getInt(1), head.firstOrNull()?.let { it.toInt() and 0xff } ?: -1,
                        if (withPayloads) head else null))
                }
            }
        }
        return Trace(total, totalBytes, entries)
    }

    fun format(state: State, trace: Trace?, now: Long): String = buildString {
        appendLine("Es wird keine eigene Bluetooth-Verbindung aufgebaut. Gezeigt werden die Notifications, die ein laufender Sync ohnehin lokal archiviert; identische Notifications nur einmal (erste Ankunft).")
        appendLine()
        when (state) {
            State.Idle -> { append("Keine Aufzeichnung gestartet."); return@buildString }
            is State.Recording -> appendLine("Aufzeichnung läuft · noch ${((state.endMs - now).coerceAtLeast(0) + 59_999) / 60_000} min (max. ${MAX_WINDOW_MS / 60_000} min)")
            is State.Finished -> appendLine("Aufzeichnung beendet · Dauer ${(state.endMs - state.startMs).coerceAtLeast(0) / 1000} s")
        }
        if (trace == null) { append("Wird geladen …"); return@buildString }
        appendLine("Notifications im Zeitfenster: ${trace.total} (${trace.totalBytes} Bytes)")
        if (trace.total == 0L) { append("Im Zeitfenster lief kein Sync, der Notifications archiviert hat."); return@buildString }
        val start = when (state) { is State.Recording -> state.startMs; is State.Finished -> state.startMs; else -> 0L }
        trace.entries.forEach { e ->
            append("+%.1f s · %d B · Kanal 0x%02X".format(java.util.Locale.GERMANY, (e.atMs - start) / 1000.0, e.bytes, e.channel.coerceAtLeast(0)))
            e.payload?.let { append(" · ").append(BleSafetyPolicy.safeHex(it)).append(if (e.bytes > it.size) " …" else "") }
            appendLine()
        }
        if (trace.total > trace.entries.size) appendLine("… ${trace.total - trace.entries.size} weitere nicht angezeigt (Limit $MAX_ENTRIES).")
        append(if (trace.entries.any { it.payload != null }) "Bytes sichtbar (Opt-in, höchstens ${BleSafetyPolicy.maximumDiagnosticBytes} je Notification)."
               else "Bytes ausgeblendet; nur nach separatem Opt-in sichtbar.")
    }
}
