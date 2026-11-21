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
        val status: String,
        /** The stored rows (one per source file) behind this aggregated entry; needed to change the review status (#204). */
        val records: List<StoredFitUnmappedField> = emptyList()
    )

    /** All field types of one FIT message number, for a collapsible list (#204: 930 single rows are unreadable). */
    data class Group(val messageNumber: Int, val title: String, val entries: List<Entry>)

    data class Summary(
        val typeCount: Int, val entries: List<Entry>, val omitted: Int, val groups: List<Group> = emptyList(),
        val headline: String = "", val explanation: String = ""
    ) {
        val isEmpty get() = typeCount == 0
    }

    const val MAX_ENTRIES = 100

    private data class Key(
        val global: Int, val kind: StoredFitFieldKind, val field: Int, val baseType: Int?,
        val size: Int, val developerIndex: Int?, val classification: String
    )

    fun summarize(strings: Strings, fields: List<StoredFitUnmappedField>): Summary {
        val grouped = fields.groupBy {
            Key(it.globalMessageNumber, it.kind, it.fieldNumber, it.baseTypeCode, it.fieldSize, it.developerDataIndex, it.classification)
        }
        val entries = grouped.entries
            .sortedWith(compareBy({ it.key.global }, { it.key.kind }, { it.key.field }, { it.key.developerIndex ?: -1 }))
            .map { (key, rows) ->
                val files = rows.map { it.sourceSha256 }.distinct().size
                val messages = rows.sumOf { it.observedMessageCount }
                key.global to Entry(
                    title = strings.get(if (key.kind == StoredFitFieldKind.DEVELOPER) R.string.unk_title_dev else R.string.unk_title_std, key.global, key.field),
                    detail = key.baseType?.toString().let { base ->
                        val type = base ?: strings.get(R.string.unk_base_unknown)
                        key.developerIndex?.let { strings.get(R.string.unk_detail_dev, type, key.size, it) } ?: strings.get(R.string.unk_detail, type, key.size)
                    },
                    occurrence = strings.get(when {
                        messages == 1L && files == 1 -> R.string.unk_occ_11
                        messages == 1L -> R.string.unk_occ_1n
                        files == 1 -> R.string.unk_occ_n1
                        else -> R.string.unk_occ_nn
                    }, messages, files),
                    classification = classificationLabel(strings, key.classification),
                    status = statusLabel(strings, rows.map { it.status }.distinct()),
                    records = rows
                )
            }
        val entryList = entries.map { it.second }
        val groups = grouped.entries.groupBy { it.key.global }.toSortedMap().map { (message, parts) ->
            val fieldEntries = entries.filter { it.first == message }.map { it.second }
            val occurrences = parts.sumOf { p -> p.value.sumOf { it.observedMessageCount } }
            Group(message, strings.get(when {
                parts.size == 1 && occurrences == 1L -> R.string.unk_group_11
                parts.size == 1 -> R.string.unk_group_1n
                occurrences == 1L -> R.string.unk_group_n1
                else -> R.string.unk_group_nn
            }, message, parts.size, occurrences), fieldEntries)
        }
        val headline = if (entryList.size == 1) strings.get(R.string.unk_headline_one) else strings.get(R.string.unk_headline_many, entryList.size)
        return Summary(entryList.size, entryList.take(MAX_ENTRIES), (entryList.size - MAX_ENTRIES).coerceAtLeast(0), groups,
            headline, strings.get(R.string.unk_explanation))
    }

    fun classificationLabel(strings: Strings, code: String) = strings.get(when (code) {
        "MESSAGE_NOT_DECODED_BY_APP" -> R.string.unk_cls_message
        "FIELD_NOT_DECODED_BY_APP" -> R.string.unk_cls_field
        "KNOWN_MAPPING_INCOMPATIBLE" -> R.string.unk_cls_incompatible
        "DEVELOPER_FIELD_NOT_DECODED_BY_APP" -> R.string.unk_cls_developer
        else -> R.string.unk_cls_other
    })

    fun statusLabel(strings: Strings, statuses: List<FitFieldInventoryStatus>): String =
        statuses.joinToString(" / ") { strings.get(statusRes(it)) }

    fun statusRes(status: FitFieldInventoryStatus): Int = when (status) {
        FitFieldInventoryStatus.UNREVIEWED -> R.string.unk_status_unreviewed
        FitFieldInventoryStatus.INVESTIGATING -> R.string.unk_status_investigating
        FitFieldInventoryStatus.SUPPORTED -> R.string.unk_status_supported
        FitFieldInventoryStatus.IGNORED -> R.string.unk_status_ignored
    }
}
