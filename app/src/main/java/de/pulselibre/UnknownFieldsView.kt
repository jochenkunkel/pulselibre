package de.pulselibre

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

/**
 * Subtle hint on the device page: "Unbekannte Datenfelder entdeckt (N Typen)". Hidden when there is nothing to show.
 * Tapping opens the read-only list (coordinates, counters, classification, status); no raw values, no medical labels.
 */
internal class UnknownFieldsView(context: Context) : LinearLayout(context), TabAwareVisibility {
    override var tabShown = false
        set(value) { field = value; visibility = TabAwareVisibility.visibility(value, hasContent) }
    override val hasContent: Boolean get() = summary?.isEmpty == false
    private val palette = AppPalette.of(context)
    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()
    private var summary: UnknownFieldsSummary.Summary? = null
    private val strings = Strings.of(context)

    /** Called after a review status was stored, so the host can refresh the hint (#204). */
    var onStatusChanged: (() -> Unit)? = null

    init { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; visibility = View.GONE; tag = "unknown_fields_hint" }

    fun show(value: UnknownFieldsSummary.Summary) {
        summary = value
        removeAllViews()
        visibility = TabAwareVisibility.visibility(tabShown, hasContent)
        if (value.isEmpty) return
        background = GradientDrawable().apply { setColor(palette.tinted); cornerRadius = dp(14).toFloat() }
        setPadding(dp(14), dp(12), dp(14), dp(12))
        isClickable = true; isFocusable = true
        contentDescription = context.getString(R.string.unk_hint_cd, value.headline, value.explanation)
        addView(PlIcon.view(context, PlIcon.DATA, 22, palette.accent), LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(12) })
        val texts = LinearLayout(context).apply { orientation = VERTICAL }
        texts.addView(TextView(context).apply {
            text = value.headline; textSize = 14f; setTextColor(palette.ink); setTypeface(null, Typeface.BOLD)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        })
        texts.addView(TextView(context).apply {
            text = value.explanation; textSize = 12f; setTextColor(palette.muted)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        })
        addView(texts, LayoutParams(0, -2, 1f))
        setOnClickListener { showDetails(value) }
    }

    private fun showDetails(value: UnknownFieldsSummary.Summary) {
        val content = LinearLayout(context).apply { orientation = VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(8)) }
        content.addView(TextView(context).apply {
            text = context.getString(R.string.unk_intro)
            textSize = 13f; setTextColor(palette.muted); setPadding(0, 0, 0, dp(10))
        })
        // Grouped by FIT message number; a group's field list is inflated lazily on first expand (keeps 900+ types cheap).
        fun entryView(e: UnknownFieldsSummary.Entry) = LinearLayout(context).apply {
            orientation = VERTICAL; setPadding(dp(12), dp(6), 0, dp(6)); tag = "unknown_field_entry"
            addView(TextView(context).apply { text = e.title; textSize = 13f; setTextColor(palette.ink); setTypeface(null, Typeface.BOLD) })
            addView(TextView(context).apply { text = e.detail; textSize = 12f; setTextColor(palette.muted) })
            addView(TextView(context).apply { text = e.occurrence; textSize = 12f; setTextColor(palette.muted) })
            val statusLine = TextView(context).apply {
                text = context.getString(R.string.unk_status_line, e.classification, e.status); textSize = 12f; setTextColor(palette.muted)
            }
            addView(statusLine)
            if (e.records.isNotEmpty()) {
                // #204: der Status ist eine eigene Prüfmarke; er ändert keine Messdaten.
                addView(TextView(context).apply {
                    text = context.getString(R.string.unk_status_change); textSize = 12f; setTextColor(palette.accent); setTypeface(null, Typeface.BOLD)
                    minimumHeight = dp(48); gravity = Gravity.CENTER_VERTICAL; isClickable = true; isFocusable = true
                    tag = "unknown_field_status_change"; contentDescription = context.getString(R.string.unk_status_change_cd, e.title)
                    setOnClickListener { chooseStatus(e, statusLine) }
                })
                // #204 item 8: raw example bytes only after an explicit, warned action; nothing is stored or sent.
                addView(TextView(context).apply {
                    text = context.getString(R.string.unk_bytes_action); textSize = 12f; setTextColor(palette.accent); setTypeface(null, Typeface.BOLD)
                    minimumHeight = dp(48); gravity = Gravity.CENTER_VERTICAL; isClickable = true; isFocusable = true
                    tag = "unknown_field_bytes_preview"; contentDescription = context.getString(R.string.unk_bytes_action_cd, e.title)
                    setOnClickListener { confirmBytesPreview(e) }
                })
            }
        }
        value.groups.forEach { g ->
            val body = LinearLayout(context).apply { orientation = VERTICAL; visibility = View.GONE; tag = "unknown_group_body_${g.messageNumber}" }
            var inflated = false
            val header = TextView(context).apply {
                text = "▸  ${g.title}"; textSize = 14f; setTextColor(palette.ink); setTypeface(null, Typeface.BOLD)
                setPadding(0, dp(12), 0, dp(12)); minimumHeight = dp(48); gravity = Gravity.CENTER_VERTICAL
                isClickable = true; isFocusable = true; tag = "unknown_group_${g.messageNumber}"
                contentDescription = context.getString(R.string.unk_group_cd, g.title)
                setOnClickListener {
                    if (!inflated) { g.entries.forEach { body.addView(entryView(it)) }; inflated = true }
                    val open = body.visibility != View.VISIBLE
                    body.visibility = if (open) View.VISIBLE else View.GONE
                    text = (if (open) "▾  " else "▸  ") + g.title
                }
            }
            content.addView(header); content.addView(body)
        }
        DialogProbe.track(AlertDialog.Builder(context)
            .setTitle(value.headline)
            .setView(ScrollView(context).apply { addView(content) })
            .setPositiveButton(R.string.action_close, null)
            .create()).show()
    }

    private fun confirmBytesPreview(e: UnknownFieldsSummary.Entry) {
        DialogProbe.track(AlertDialog.Builder(context)
            .setTitle(R.string.unk_bytes_title)
            .setMessage(R.string.unk_bytes_warning)
            .setPositiveButton(R.string.unk_bytes_confirm) { _, _ -> showBytesPreview(e) }
            .setNegativeButton(R.string.action_cancel, null)
            .create()).show()
    }

    private fun showBytesPreview(e: UnknownFieldsSummary.Entry) {
        Thread {
            val root = java.io.File(context.applicationContext.filesDir, "verified_fit")
            val text = e.records.map { it.sourceSha256 }.distinct().take(MAX_PREVIEW_FILES).withIndex().joinToString("\n\n") { (i, sha) ->
                val record = e.records.first { it.sourceSha256 == sha }
                val file = java.io.File(root, "$sha.fit")
                val safe = SHA_FILE.matches(sha) && file.isFile && !java.nio.file.Files.isSymbolicLink(file.toPath())
                val preview = if (!safe) null else try {
                    FitFieldSampleReader.read(file.readBytes(), record.globalMessageNumber, record.fieldNumber,
                        if (record.kind == StoredFitFieldKind.DEVELOPER) record.developerDataIndex else null)
                } catch (_: Exception) { null }
                FitFieldSampleReader.describe(strings, i + 1, preview)
            }
            val more = e.records.map { it.sourceSha256 }.distinct().size - MAX_PREVIEW_FILES
            post {
                val body = TextView(context).apply {
                    this.text = text + if (more > 0) "\n\n" + context.getString(R.string.unk_bytes_more_files, more) else ""
                    textSize = 13f; typeface = Typeface.MONOSPACE; setTextColor(palette.ink); tag = "unknown_field_bytes_text"
                    setPadding(dp(20), dp(8), dp(20), dp(8))
                }
                DialogProbe.track(AlertDialog.Builder(context)
                    .setTitle(e.title)
                    .setView(ScrollView(context).apply { addView(body) })
                    .setPositiveButton(R.string.action_close, null)
                    .create()).show()
            }
        }.start()
    }

    private companion object {
        const val MAX_PREVIEW_FILES = 3
        val SHA_FILE = Regex("[0-9a-f]{64}")
    }

    private val statusChoices = FitFieldInventoryStatus.entries.toList()

    private fun chooseStatus(e: UnknownFieldsSummary.Entry, statusLine: TextView) {
        val current = e.records.map { it.status }.distinct().singleOrNull()
        lateinit var dialog: AlertDialog
        // Eigene Ansicht statt setMessage + setSingleChoiceItems: AppCompat gibt der Nachricht den ganzen Platz und versteckt die Liste.
        val content = LinearLayout(context).apply { orientation = VERTICAL; setPadding(dp(24), dp(8), dp(24), dp(8)) }
        content.addView(TextView(context).apply {
            text = context.getString(R.string.unk_status_note)
            textSize = 13f; setTextColor(palette.muted); setPadding(0, 0, 0, dp(8))
        })
        statusChoices.forEach { status ->
            val label = context.getString(UnknownFieldsSummary.statusRes(status))
            content.addView(TextView(context).apply {
                text = (if (status == current) "●  " else "○  ") + label
                textSize = 15f; setTextColor(palette.ink); minimumHeight = dp(48); gravity = Gravity.CENTER_VERTICAL
                isClickable = true; isFocusable = true; tag = "unknown_status_${status.name.lowercase()}"
                contentDescription = if (status == current) context.getString(R.string.unk_status_selected_cd, label) else label
                setOnClickListener {
                    dialog.dismiss()
                    Thread {
                        val ok = try {
                            CirqaDatabase(context.applicationContext).use { db -> e.records.forEach { db.updateFitUnmappedFieldStatus(it, status) } }
                            true
                        } catch (_: Exception) { false }
                        statusLine.post {
                            statusLine.text = if (ok) context.getString(R.string.unk_status_line, e.classification, UnknownFieldsSummary.statusLabel(strings, listOf(status)))
                                else context.getString(R.string.unk_status_save_failed, e.classification)
                            if (ok) onStatusChanged?.invoke()
                        }
                    }.start()
                }
            })
        }
        dialog = DialogProbe.track(AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.unk_status_title, e.title))
            .setView(content)
            .setNegativeButton(R.string.action_cancel, null)
            .create())
        dialog.show()
    }
}
