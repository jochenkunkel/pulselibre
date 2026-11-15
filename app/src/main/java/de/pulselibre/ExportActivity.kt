package de.pulselibre

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.Executors

/** User-initiated local CSV export. Never transmits measurements over the network. */
class ExportActivity : AppCompatActivity() {
    private var from = LocalDate.now().minusDays(30)
    private var through = LocalDate.now()
    private val zone = ZoneId.systemDefault()
    private lateinit var fromButton: Button
    private lateinit var throughButton: Button
    private lateinit var feedback: TextView
    private val executor = Executors.newSingleThreadExecutor()
    private var pendingWindow: ExportWindow? = null

    private val createDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val window = pendingWindow
        pendingWindow = null
        if (uri == null || window == null) {
            feedback.text = "Export abgebrochen."
        } else {
            feedback.text = "Export wird gespeichert …"
            executor.execute {
                try {
                    val records = LocalReadingStore(this).use { it.readWindow(window) }
                    val stream = contentResolver.openOutputStream(uri)
                        ?: throw IllegalStateException("Datei kann nicht geöffnet werden")
                    stream.use { writeRawExport(it, records, window) }
                    runOnUiThread {
                        feedback.text = "CSV gespeichert: ${records.size} Messungen. Enthält nur lokal verfügbare Daten."
                    }
                } catch (error: Exception) {
                    runOnUiThread {
                        feedback.text = "Export fehlgeschlagen. Bitte Speicherort prüfen und erneut versuchen."
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        root.addView(TextView(this).apply { text = "Einstellungen · Datenexport"; textSize = 24f })
        root.addView(TextView(this).apply {
            text = "Alle lokal vorhandenen Rohmessungen im gewählten Zeitraum als CSV speichern. Keine Cloud."
        })
        fromButton = Button(this).apply { setOnClickListener { pickDate(from) { from = it; updateLabels() } } }
        throughButton = Button(this).apply { setOnClickListener { pickDate(through) { through = it; updateLabels() } } }
        root.addView(fromButton)
        root.addView(throughButton)
        feedback = TextView(this).apply { text = "Zeitzone: ${zone.id}. Der Endtag ist eingeschlossen." }
        root.addView(feedback)
        root.addView(Button(this).apply {
            text = "CSV exportieren"
            setOnClickListener {
                try {
                    pendingWindow = exportWindowForDates(from, through, zone)
                    createDocument.launch("pulselibre_${from}_bis_${through}.csv")
                } catch (_: IllegalArgumentException) {
                    feedback.text = "Ungültiger Zeitraum: Das Enddatum darf nicht vor dem Startdatum liegen."
                }
            }
        })
        updateLabels()
        setContentView(root)
    }

    private fun updateLabels() {
        fromButton.text = "Von: $from"
        throughButton.text = "Bis einschließlich: $through"
    }

    private fun pickDate(current: LocalDate, onSelected: (LocalDate) -> Unit) {
        DatePickerDialog(this, { _, year, month, day ->
            onSelected(LocalDate.of(year, month + 1, day))
        }, current.year, current.monthValue - 1, current.dayOfMonth).show()
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
