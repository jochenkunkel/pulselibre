package de.pulselibre

import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.view.Gravity
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
        fun heading(label: String) {
            layout.addView(TextView(context).apply {
                text = label
                textSize = 18f
                setTextColor(AppPalette.of(context).accent)
                setPadding(0, 18, 0, 8)
            })
        }
        fun field(label: String, key: String, numeric: Boolean = false, decimal: Boolean = false): EditText {
            layout.addView(TextView(context).apply { text = label; textSize = 14f })
            return EditText(context).apply {
                setSingleLine(true)
                inputType = if (numeric) InputType.TYPE_CLASS_NUMBER or
                    (if (decimal) InputType.TYPE_NUMBER_FLAG_DECIMAL else 0)
                    else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                setText(p.getString(key, "").orEmpty())
                hint = label
                layout.addView(this)
            }
        }
        fun choice(label: String, key: String, options: List<String>): Spinner {
            layout.addView(TextView(context).apply { text = label; textSize = 14f })
            return Spinner(context).apply {
                adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, options)
                setSelection(options.indexOf(p.getString(key, options[0])).coerceAtLeast(0))
                layout.addView(this)
            }
        }
        heading("Darstellung")
        layout.addView(TextView(context).apply {
            text = "Design: automatisch nach Android oder dauerhaft überschreiben"
        })
        val themeChoice = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item,
                ThemePreference.entries.map { it.label })
            setSelection(AppAppearance.preference(context).ordinal)
            contentDescription = "App-Design auswählen"
            layout.addView(this)
        }
        heading("Persönliches Profil")
        val name = field("Anzeigename (optional)", "display_name")
        val genderOptions = listOf("Keine Angabe", "Männlich", "Weiblich", "Divers / andere")
        val gender = choice("Geschlecht (optional; nur für geeignete Schätzformeln)", "gender", genderOptions)
        val birthYear = field("Geburtsjahr (optional)", "birth_year", true)
        val age = field("Alter in Jahren (optional)", "age_years", true)
        heading("Körperdaten")
        val height = field("Größe in cm", "height_cm", true)
        val weight = field("Gewicht in kg", "weight_kg", true, true)
        val stepGoal = field("Tägliches Schrittziel", "step_goal", true)
        heading("Persönliche Einstellungen")
        val handedness = choice("Trageseite", "wrist", listOf("Nicht angegeben", "Links", "Rechts"))
        val units = choice("Einheiten", "units", listOf("Metrisch (kg, cm)", "Imperial (Anzeige später)"))
        val sleepGoal = field("Schlafziel in Stunden (optional)", "sleep_goal_hours", true, true)
        val resting = field("Manuell bekannter Ruhepuls (optional, bpm)", "resting_hr", true)
        val notes = field("Notiz (optional, nur lokal)", "profile_note")
        layout.addView(TextView(context).apply {
            text = "Der Körperprofil-Avatar richtet sich nach der freiwilligen Geschlechtsauswahl: Mann, Frau oder ein kleiner Jedi für Divers / andere. Ohne Angabe bleibt die neutrale Figur. Kein Benutzerkonto und keine Cloud. Die Angaben sind noch nicht mit medizinischen oder kalorischen Schätzformeln verknüpft. Das Geburtsjahr dient nur als Profilangabe; das Alter wird nicht automatisch daraus berechnet."
            textSize = 12f
            setPadding(0, 20, 0, 0)
        })
        val dialog = AlertDialog.Builder(context)
            .setTitle("⚙ Einstellungen · Mein Profil")
            .setView(scroll)
            .setNegativeButton("Schließen", null)
            .setNeutralButton("Profil löschen", null)
            .setPositiveButton("Speichern", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Profil wirklich löschen?")
                    .setMessage("Nur deine Profilangaben werden entfernt. Messdaten bleiben erhalten.")
                    .setNegativeButton("Abbrechen", null)
                    .setPositiveButton("Profil löschen") { _, _ ->
                        p.edit().clear().apply()
                        onSaved()
                        dialog.dismiss()
                    }.show()
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                fun intValue(v: EditText, min: Int, max: Int): Boolean =
                    v.text.isBlank() || v.text.toString().toIntOrNull()?.let { it in min..max } == true
                fun decimalValue(v: EditText, min: Double, max: Double): Boolean =
                    v.text.isBlank() || v.text.toString().replace(',', '.').toDoubleOrNull()?.let { it in min..max } == true
                if (!intValue(height, 50, 250) || !decimalValue(weight, 15.0, 400.0) ||
                    !intValue(age, 1, 120) || !intValue(birthYear, 1900, 2100) ||
                    !intValue(stepGoal, 100, 100000) || !decimalValue(sleepGoal, 1.0, 16.0) ||
                    !intValue(resting, 25, 220)) {
                    Toast.makeText(context, "Bitte die Zahlen prüfen (Größe, Gewicht, Alter, Ziele).", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val edit = p.edit()
                fun save(key: String, value: String) { edit.putString(key, value) }
                save("display_name", name.text.toString().trim().take(60))
                save("gender", gender.selectedItem.toString())
                save("birth_year", birthYear.text.toString().trim())
                save("age_years", age.text.toString().trim())
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
        dialog.show()
    }
}
