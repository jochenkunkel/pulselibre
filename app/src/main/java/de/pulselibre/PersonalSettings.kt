package de.pulselibre

import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog

/** Single-user, offline preferences. No account, network request, or cloud synchronization. */
object PersonalSettings {
    private const val STORE = "local_personal_profile"
    private fun prefs(context: Context) = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
    fun avatar(context: Context): String {
        val name = prefs(context).getString("display_name", "").orEmpty().trim()
        val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "☺"
        return initial
    }

    /** Locally selected profile avatar; no gender is inferred from appearance. */
    fun bodyAvatar(context: Context): String = when (prefs(context).getString("gender", "Keine Angabe")) {
        "Männlich" -> "👨"
        "Weiblich" -> "👩"
        "Divers / andere" -> "🧙"
        else -> "🧍"
    }

    fun show(context: Context, onSaved: () -> Unit = {}) {
        val p = prefs(context)
        val scroll = ScrollView(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val d = (20 * resources.displayMetrics.density).toInt()
            setPadding(d, d / 2, d, d)
        }
        scroll.addView(layout)
        val palette = AppPalette.of(context)
        fun dp(v: Int) = (v * context.resources.displayMetrics.density + 0.5f).toInt()
        fun showInfo(title: String, text: String) {
            AlertDialog.Builder(context).setTitle(title).setMessage(text).setPositiveButton("OK", null).show()
        }
        /** Compact section header: icon + title, optional ⓘ opening the long explanation. */
        fun heading(icon: PlIcon, label: String, info: String? = null) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(14), 0, dp(4))
            }
            row.addView(PlIcon.view(context, icon, 24), LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(10) })
            row.addView(TextView(context).apply {
                text = label; textSize = 16f; setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(palette.accent)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            if (info != null) row.addView(FrameLayout(context).apply {
                contentDescription = "Erklärung zu $label"
                isClickable = true; isFocusable = true
                setOnClickListener { showInfo(label, info) }
                addView(PlIcon.view(context, PlIcon.INFO, 22), FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
            layout.addView(row)
        }
        val compactRows = context.resources.configuration.screenWidthDp < 340 || context.resources.configuration.fontScale >= 1.3f
        fun newField(label: String, key: String, numeric: Boolean, decimal: Boolean, unitHint: String?): Pair<LinearLayout, EditText> {
            val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            box.addView(TextView(context).apply {
                text = label; textSize = 12f; setTextColor(palette.muted)
                // Fields in one row keep their inputs aligned even if a label wraps (narrow width / large font).
                if (compactRows) minLines = 2
            })
            val edit = EditText(context).apply {
                setSingleLine(true)
                inputType = if (numeric) InputType.TYPE_CLASS_NUMBER or
                    (if (decimal) InputType.TYPE_NUMBER_FLAG_DECIMAL else 0)
                    else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                // Some keys (SpO2 scale, activity thresholds) are stored as Int by their own settings classes;
                // getString would throw ClassCastException once they were saved (crash on opening Config).
                setText(p.all[key]?.toString().orEmpty())
                hint = unitHint.orEmpty()
                minimumHeight = dp(48)
                contentDescription = label
            }
            box.addView(edit)
            return box to edit
        }
        /** Puts several fields side by side so the dialog stays short. */
        fun fieldRow(vararg specs: Triple<String, String, String?>, numeric: Boolean = true, decimal: Boolean = false): List<EditText> {
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            val edits = ArrayList<EditText>()
            specs.forEachIndexed { i, (label, key, unit) ->
                val (box, edit) = newField(label, key, numeric, decimal && i == specs.lastIndex, unit)
                edits += edit
                row.addView(box, LinearLayout.LayoutParams(0, -2, 1f).apply { if (i < specs.lastIndex) marginEnd = dp(8) })
            }
            layout.addView(row)
            return edits
        }
        fun field(label: String, key: String, numeric: Boolean = false, decimal: Boolean = false): EditText =
            fieldRow(Triple(label, key, null), numeric = numeric, decimal = decimal)[0]
        fun choice(label: String, key: String, options: List<String>): Spinner {
            layout.addView(TextView(context).apply { text = label; textSize = 12f; setTextColor(palette.muted) })
            return Spinner(context).apply {
                adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, options)
                setSelection(options.indexOf(p.getString(key, options[0])).coerceAtLeast(0))
                contentDescription = label
                layout.addView(this)
            }
        }

        heading(PlIcon.IDENTITY, "Profil",
            "Der Körperprofil-Avatar richtet sich nach der freiwilligen Geschlechtsauswahl: Mann, Frau oder ein kleiner Jedi für Divers / andere. Ohne Angabe bleibt die neutrale Figur. Kein Benutzerkonto und keine Cloud. Die Angaben sind noch nicht mit medizinischen oder kalorischen Schätzformeln verknüpft. Aus dem Geburtsdatum wird das Alter für den jeweiligen Tag berechnet; es bleibt lokal gespeichert.")
        val name = field("Anzeigename", "display_name")
        val genderOptions = listOf("Keine Angabe", "Männlich", "Weiblich", "Divers / andere")
        val gender = choice("Geschlecht (optional)", "gender", genderOptions)
        // Birth date instead of a static age (#117): the age is derived for today and never stored.
        var birthDate: java.time.LocalDate? = AgeCalculator.parse(p.getString("birth_date", ""))
        val dateFormat = java.time.format.DateTimeFormatter.ofPattern("dd.MM.uuuu")
        layout.addView(TextView(context).apply { text = "Geburtsdatum (optional)"; textSize = 12f; setTextColor(palette.muted) })
        val birthButton = Button(context).apply {
            isAllCaps = false
            minHeight = dp(48)
            setTextColor(palette.ink)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.TRANSPARENT); setStroke(dp(1), palette.muted); cornerRadius = dp(12).toFloat()
            }
            tag = "settings_birth_date"
        }
        val legacyAge = p.getString("age_years", "").orEmpty().trim()
        val legacyNote = TextView(context).apply {
            textSize = 12f; setTextColor(palette.muted)
            text = "Bisher gespeichertes Alter: $legacyAge Jahre. Bitte Geburtsdatum ergänzen; es wird nicht aus dem Alter geschätzt."
        }
        fun refreshBirth() {
            val today = java.time.LocalDate.now()
            val b = birthDate
            birthButton.text = if (b == null) "Geburtsdatum wählen"
                else "${b.format(dateFormat)}  ·  ${AgeCalculator.ageOn(b, today)?.let { "$it Jahre" } ?: "ungültig"}"
            birthButton.contentDescription = if (b == null) "Geburtsdatum wählen" else "Geburtsdatum ${b.format(dateFormat)} ändern"
            legacyNote.visibility = if (b == null && legacyAge.toIntOrNull() != null) android.view.View.VISIBLE else android.view.View.GONE
        }
        birthButton.setOnClickListener {
            val start = birthDate ?: java.time.LocalDate.now().minusYears(30) // picker start only, nothing is stored until confirmed
            android.app.DatePickerDialog(context, { _, y, m, d ->
                birthDate = java.time.LocalDate.of(y, m + 1, d); refreshBirth()
            }, start.year, start.monthValue - 1, start.dayOfMonth).apply {
                datePicker.maxDate = System.currentTimeMillis() // no future dates
            }.show()
        }
        layout.addView(birthButton, LinearLayout.LayoutParams(-1, -2))
        layout.addView(legacyNote)
        refreshBirth()

        heading(PlIcon.FITNESS, "Körper & Ziele")
        val (height, weight) = fieldRow(
            Triple("Größe (cm)", "height_cm", null), Triple("Gewicht (kg)", "weight_kg", null), decimal = true)
        val (stepGoal, sleepGoal) = fieldRow(
            Triple("Schrittziel/Tag", "step_goal", null), Triple("Schlafziel (h)", "sleep_goal_hours", null), decimal = true)
        val resting = field("Bekannter Ruhepuls (bpm)", "resting_hr", true)

        heading(PlIcon.CIRQA_BAND, "Sonstiges")
        val handedness = choice("Trageseite", "wrist", listOf("Nicht angegeben", "Links", "Rechts"))
        val units = choice("Einheiten", "units", listOf("Metrisch (kg, cm)", "Imperial (Anzeige später)"))
        val notes = field("Notiz (nur lokal)", "profile_note")
        heading(PlIcon.OVERVIEW, "Darstellung")
        val themeChoice = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item,
                ThemePreference.entries.map { it.label })
            setSelection(AppAppearance.preference(context).ordinal)
            contentDescription = "App-Design auswählen"
            layout.addView(this)
        }

        heading(PlIcon.STEPS, "Aktivität aus Schritten",
            "Aus kumulativen CIRQA-Schrittständen der letzten Minuten. Der Modus ist eine grobe Anzeige-Heuristik: Schritte belegen weder Sitzen noch eine bestimmte Sportart. Bei zu wenigen oder veralteten Werten bleibt er unbekannt.")
        val activitySettings = ActivityDetectionPreference.read(context)
        val (restSteps, sportSteps, activityWindow) = fieldRow(
            Triple("Ruhe (/min)", "activity_rest_below", null),
            Triple("Sport (/min)", "activity_sport_above", null),
            Triple("Fenster (min)", "activity_window_minutes", null))
        restSteps.setText(activitySettings.restBelowStepsPerMinute.toString())
        sportSteps.setText(activitySettings.sportAboveStepsPerMinute.toString())
        activityWindow.setText(activitySettings.windowMinutes.toString())

        val spo2Settings = SpO2DisplaySettings.read(context)
        heading(PlIcon.OXYGEN, "SpO₂-Skala · nicht validiert",
            "Vorschau ${spo2Settings.bandLabel()}\n\n${if (spo2Settings.colorBandsEnabled) "Farbskala aktiv (vom Nutzer akzeptiert); der violette Kartenrand kennzeichnet die fehlende Validierung." else "Farbskala bleibt neutral."} Der CIRQA-Handgelenksensor ist nicht gerätespezifisch validiert. Keine Diagnose oder Alarmfunktion; Signalqualität wird nicht übertragen.")
        val (spo2Green, spo2Orange) = fieldRow(
            Triple("Grün ab (%)", "spo2_display_green_from", null),
            Triple("Orange ab (%)", "spo2_display_red_below", null))
        spo2Green.setText(spo2Settings.greenFrom.toString())
        spo2Orange.setText(spo2Settings.redBelow.toString())

        // Version / build: first thing in the settings so installed builds can be told apart.
        val versionInfo = AppVersionInfo.read(context)
        layout.addView(TextView(context).apply {
            text = versionInfo.detail()
            compoundDrawablePadding = dp(10)
            setCompoundDrawablesRelativeWithIntrinsicBounds(PlIcon.drawable(context, PlIcon.FIRMWARE, 22), null, null, null)
            textSize = 14f
            setTextColor(palette.ink)
            setTypeface(null, android.graphics.Typeface.BOLD)
            tag = "settings_version"
            contentDescription = "App-Version ${versionInfo.detail()}"
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(palette.tinted); cornerRadius = dp(12).toFloat()
            }
        })

        layout.addView(TextView(context).apply {
            text = "Entwickelt von Jochen Kunkel"
            compoundDrawablePadding = dp(8)
            setCompoundDrawablesRelativeWithIntrinsicBounds(PlIcon.drawable(context, PlIcon.IDENTITY, 18, palette.muted), null, null, null)
            textSize = 13f
            setTextColor(palette.muted)
            tag = "settings_developer"
            contentDescription = "Entwickler: Jochen Kunkel"
            setPadding(dp(4), dp(8), dp(4), 0)
        })
        // Explanation of the violet tile border (moved here from the dashboard): calm info bar, full wording kept.
        layout.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(palette.tinted); cornerRadius = dp(14).toFloat()
                setStroke(dp(1), UnvalidatedFeatures.color(palette.dark))
            }
            tag = "settings_unvalidated_note"
            contentDescription = UnvalidatedFeatures.LEGEND
            addView(PlIcon.view(context, PlIcon.INFO, 22), LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(10) })
            addView(TextView(context).apply { text = UnvalidatedFeatures.LEGEND; textSize = 12f; setTextColor(palette.ink)
                importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO }, LinearLayout.LayoutParams(0, -2, 1f))
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        // Destructive action sits apart at the end of the scrolling content, never next to "Speichern".
        heading(PlIcon.STORAGE, "Profil löschen")
        val deleteButton = Button(context).apply {
            text = "Profil löschen …"
            isAllCaps = false
            contentDescription = "Profil löschen, erfordert zwei Bestätigungen"
            setTextColor(palette.ink)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.TRANSPARENT); setStroke(dp(1), palette.muted); cornerRadius = dp(12).toFloat()
            }
            minHeight = dp(48)
        }
        layout.addView(deleteButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4); bottomMargin = dp(8) })

        fun barButton(label: String, filled: Boolean) = Button(context).apply {
            text = label
            isAllCaps = false
            textSize = 15f
            minHeight = dp(48)
            setTextColor(if (filled) Color.WHITE else palette.accent)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(if (filled) palette.primary else palette.tinted); cornerRadius = dp(12).toFloat()
            }
        }
        val closeButton = barButton("Schließen", false)
        val saveButton = barButton("Speichern", true)
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(8), dp(16), dp(12))
            addView(closeButton, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) })
            addView(saveButton, LinearLayout.LayoutParams(0, -2, 1f))
        }
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(bar, LinearLayout.LayoutParams(-1, -2))
            layoutParams = ViewGroup.LayoutParams(-1, (context.resources.displayMetrics.heightPixels * 0.78f).toInt())
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle("Einstellungen")
            .setView(container)
            .create()
        closeButton.setOnClickListener { dialog.dismiss() }
        dialog.setOnShowListener {
            deleteButton.setOnClickListener {
                // Two separate confirmations: scope first, then the irreversible final step.
                DialogProbe.track(AlertDialog.Builder(context)
                    .setTitle("Profil löschen?")
                    .setMessage("Alle Profilangaben (Name, Körperdaten, Ziele, Einstellungen) werden entfernt. Messdaten bleiben erhalten.")
                    .setNegativeButton("Abbrechen", null)
                    .setPositiveButton("Weiter") { _, _ ->
                        DialogProbe.track(AlertDialog.Builder(context)
                            .setTitle("Wirklich endgültig löschen?")
                            .setMessage("Das Löschen kann nicht rückgängig gemacht werden.")
                            .setNegativeButton("Abbrechen", null)
                            .setPositiveButton("Endgültig löschen") { _, _ ->
                                p.edit().clear().apply()
                                onSaved()
                                dialog.dismiss()
                            }.create()).show()
                    }.create()).show()
            }
            saveButton.setOnClickListener {
                fun intValue(v: EditText, min: Int, max: Int): Boolean =
                    v.text.isBlank() || v.text.toString().toIntOrNull()?.let { it in min..max } == true
                fun decimalValue(v: EditText, min: Double, max: Double): Boolean =
                    v.text.isBlank() || v.text.toString().replace(',', '.').toDoubleOrNull()?.let { it in min..max } == true
                if (!intValue(height, 50, 250) || !decimalValue(weight, 15.0, 400.0) ||
                    !intValue(stepGoal, 100, 100000) || !decimalValue(sleepGoal, 1.0, 16.0) ||
                    !intValue(resting, 25, 220) ||
                    restSteps.text.toString().toIntOrNull()?.let { it !in 0..99 } != false ||
                    sportSteps.text.toString().toIntOrNull()?.let { it !in 1..300 } != false ||
                    activityWindow.text.toString().toIntOrNull()?.let { it !in 2..15 } != false ||
                    (restSteps.text.toString().toIntOrNull() ?: 100) >= (sportSteps.text.toString().toIntOrNull() ?: 0) ||
                    spo2Green.text.toString().toIntOrNull()?.let { it !in 2..100 } != false ||
                    spo2Orange.text.toString().toIntOrNull()?.let { it !in 1 until (spo2Green.text.toString().toIntOrNull() ?: 0) } != false) {
                    Toast.makeText(context, "Bitte die Zahlen prüfen (Größe, Gewicht, Ziele und SpO₂-Orientierungsbereiche).", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                ActivityDetectionPreference.save(context, ActivityDetectionSettings(
                    restBelowStepsPerMinute = restSteps.text.toString().toInt(),
                    sportAboveStepsPerMinute = sportSteps.text.toString().toInt(),
                    windowMinutes = activityWindow.text.toString().toInt()
                ))
                SpO2DisplaySettings.save(context, SpO2DisplaySettings(
                    greenFrom = spo2Green.text.toString().toInt(),
                    redBelow = spo2Orange.text.toString().toInt()
                ))
                val edit = p.edit()
                fun save(key: String, value: String) { edit.putString(key, value) }
                save("display_name", name.text.toString().trim().take(60))
                save("gender", gender.selectedItem.toString())
                // age_years / birth_year of older versions stay untouched; only a chosen birth date is written.
                birthDate?.let { save("birth_date", it.toString()) }
                save("height_cm", height.text.toString().trim())
                save("weight_kg", weight.text.toString().trim().replace(',', '.'))
                save("step_goal", stepGoal.text.toString().trim())
                save("wrist", handedness.selectedItem.toString())
                save("units", units.selectedItem.toString())
                save("sleep_goal_hours", sleepGoal.text.toString().trim().replace(',', '.'))
                save("resting_hr", resting.text.toString().trim())
                save("profile_note", notes.text.toString().trim().take(300))
                edit.apply()
                onSaved()
                Toast.makeText(context, "Profil lokal gespeichert", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                AppAppearance.save(context, ThemePreference.entries[themeChoice.selectedItemPosition])
            }
        }
        DialogProbe.track(dialog).show()
    }
}
