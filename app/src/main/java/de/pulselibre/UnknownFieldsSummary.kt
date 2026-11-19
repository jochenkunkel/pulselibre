package de.pulselibre

/**
 * Presentation model for the stored, not-yet-decoded FIT field inventory (#204 phase B).
 * Metadata only: coordinates, counters and classification. Never a medical label and never a value.
 */
internal object UnknownFieldsSummary {
    data class Entry(
        val title: String,        // e.g. "FIT-Nachricht 20 · Feld 85"
        val detail: String,       // type / size / developer index
        val occurrence: String,   // "12 Meldungen in 3 Dateien"
        val classification: String,
        val status: String
    )

    data class Summary(val typeCount: Int, val entries: List<Entry>, val omitted: Int) {
        val isEmpty get() = typeCount == 0
        val headline get() = if (typeCount == 1) "Unbekanntes Datenfeld entdeckt (1 Typ)" else "Unbekannte Datenfelder entdeckt ($typeCount Typen)"
        val explanation get() = "Von der App noch nicht entschlüsselt. Das sind keine Messwerte und keine Diagnose."
    }

    const val MAX_ENTRIES = 100

    private data class Key(
        val global: Int, val kind: StoredFitFieldKind, val field: Int, val baseType: Int?,
        val size: Int, val developerIndex: Int?, val classification: String
    )

    fun summarize(fields: List<StoredFitUnmappedField>): Summary {
        val grouped = fields.groupBy {
            Key(it.globalMessageNumber, it.kind, it.fieldNumber, it.baseTypeCode, it.fieldSize, it.developerDataIndex, it.classification)
        }
        val entries = grouped.entries
            .sortedWith(compareBy({ it.key.global }, { it.key.kind }, { it.key.field }, { it.key.developerIndex ?: -1 }))
            .map { (key, rows) ->
                val files = rows.map { it.sourceSha256 }.distinct().size
                val messages = rows.sumOf { it.observedMessageCount }
                Entry(
                    title = "FIT-Nachricht ${key.global} · " +
                        (if (key.kind == StoredFitFieldKind.DEVELOPER) "Developer-Feld" else "Feld") + " ${key.field}",
                    detail = "Basistyp ${key.baseType?.toString() ?: "unbekannt"} · ${key.size} Byte" +
                        (key.developerIndex?.let { " · Developer-Index $it" } ?: ""),
                    occurrence = "$messages ${if (messages == 1L) "Meldung" else "Meldungen"} in $files ${if (files == 1) "Datei" else "Dateien"}",
                    classification = classificationLabel(key.classification),
                    status = statusLabel(rows.map { it.status }.distinct())
                )
            }
        return Summary(entries.size, entries.take(MAX_ENTRIES), (entries.size - MAX_ENTRIES).coerceAtLeast(0))
    }

    fun classificationLabel(code: String) = when (code) {
        "MESSAGE_NOT_DECODED_BY_APP" -> "Nachrichtentyp von der App nicht dekodiert"
        "FIELD_NOT_DECODED_BY_APP" -> "Feld von der App nicht dekodiert"
        "KNOWN_MAPPING_INCOMPATIBLE" -> "Bekanntes Feld mit unpassendem Datentyp"
        "DEVELOPER_FIELD_NOT_DECODED_BY_APP" -> "Developer-Feld nicht dekodiert"
        else -> "Nicht dekodiert"
    }

    private fun statusLabel(statuses: List<FitFieldInventoryStatus>): String =
        statuses.joinToString(" / ") {
            when (it) {
                FitFieldInventoryStatus.UNREVIEWED -> "ungeprüft"
                FitFieldInventoryStatus.INVESTIGATING -> "in Prüfung"
                FitFieldInventoryStatus.SUPPORTED -> "unterstützt"
                FitFieldInventoryStatus.IGNORED -> "ignoriert"
            }
        }
}
