package de.pulselibre

/**
 * User-defined layout of the overview tiles (#267): show/hide, order and favourites ("Im Fokus").
 * Pure and Android-free. Hiding a tile never stops data collection; it only changes what the overview shows.
 */
internal object DashboardLayout {
    const val HEART = "Herzfrequenz"
    const val SPO2 = "Sauerstoffsättigung · SpO₂"
    const val SLEEP = "Schlaf"
    const val NIGHT = "Nacht-Insights"
    const val STEPS = "Schritte (Tageszähler)"

    /** Tiles shown full width; all others share a row in pairs. */
    val FULL_WIDTH = setOf(SLEEP, NIGHT, STEPS)

    data class Entry(val key: String, val visible: Boolean = true, val favorite: Boolean = false)

    fun defaultKeys(details: List<String>): List<String> = listOf(HEART, SPO2, SLEEP, NIGHT, STEPS) + details

    /**
     * Stored entries mapped onto the keys that exist now: stored order and flags are kept for known keys, keys that
     * are new appear at the end (visible), keys that no longer exist are dropped (a vanished tile is not remembered).
     */
    fun resolve(stored: List<Entry>, available: List<String>): List<Entry> {
        val known = stored.filter { it.key in available }.distinctBy { it.key }
        return known + available.filter { key -> known.none { it.key == key } }.map { Entry(it) }
    }

    /** `true` when the layout equals the built-in one, so the original screen structure applies unchanged. */
    fun isDefault(entries: List<Entry>, available: List<String>): Boolean =
        entries == available.map { Entry(it) }

    /** Visible tiles in display order: favourites first, otherwise the user's order (stable). */
    fun displayOrder(entries: List<Entry>): List<Entry> {
        val visible = entries.filter { it.visible }
        return visible.filter { it.favorite } + visible.filterNot { it.favorite }
    }

    fun moveUp(entries: List<Entry>, key: String): List<Entry> = move(entries, key, -1)
    fun moveDown(entries: List<Entry>, key: String): List<Entry> = move(entries, key, +1)

    private fun move(entries: List<Entry>, key: String, delta: Int): List<Entry> {
        val i = entries.indexOfFirst { it.key == key }
        val j = i + delta
        if (i < 0 || j !in entries.indices) return entries
        return entries.toMutableList().also { val t = it[i]; it[i] = it[j]; it[j] = t }
    }

    fun toggleVisible(entries: List<Entry>, key: String) =
        entries.map { if (it.key == key) it.copy(visible = !it.visible) else it }

    fun toggleFavorite(entries: List<Entry>, key: String) =
        entries.map { if (it.key == key) it.copy(favorite = !it.favorite) else it }

    /** Rows of the custom layout: half-width tiles pair up, a full-width tile closes the open row and stands alone. */
    fun rows(order: List<Entry>): List<List<String>> {
        val out = mutableListOf<List<String>>()
        var open = mutableListOf<String>()
        fun flush() { if (open.isNotEmpty()) { out += open; open = mutableListOf() } }
        for (e in order) {
            if (e.key in FULL_WIDTH) { flush(); out += listOf(e.key) }
            else { open += e.key; if (open.size == 2) flush() }
        }
        flush()
        return out
    }

    private const val SEP = '\u001f'
    private const val FIELD = '\u001e'

    fun encode(entries: List<Entry>): String =
        entries.joinToString(SEP.toString()) { "${it.key}$FIELD${if (it.visible) 1 else 0}$FIELD${if (it.favorite) 1 else 0}" }

    /** Tolerant: malformed parts are skipped, so a damaged preference falls back to the built-in layout. */
    fun decode(raw: String?): List<Entry> = raw.orEmpty().split(SEP).mapNotNull { part ->
        val f = part.split(FIELD)
        if (f.size != 3 || f[0].isEmpty() || f[1] !in listOf("0", "1") || f[2] !in listOf("0", "1")) null
        else Entry(f[0], f[1] == "1", f[2] == "1")
    }.distinctBy { it.key }
}
