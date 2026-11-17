package de.pulselibre

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class ExportActivityTest {
    @Test fun exportScreenShowsDateRangeAndExportButton() {
        ActivityScenario.launch(ExportActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                val fields = mutableListOf<TextView>()
                fun visit(view: View) {
                    if (view is TextView) fields += view
                    if (view is ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i))
                }
                visit(root)
                val labels = fields.map { it.text.toString() }
                assertTrue("Export title missing", labels.contains("Datenexport"))
                assertTrue("Start label missing", labels.contains("Von"))
                assertTrue("End label missing", labels.contains("Bis"))
                val dates = fields.filter {
                    it.isClickable && runCatching { LocalDate.parse(it.text) }.isSuccess
                }
                assertEquals("Both date selectors must be available", 2, dates.size)
                assertTrue(LocalDate.parse(dates[0].text) <= LocalDate.parse(dates[1].text))
                assertTrue(fields.any { it.isClickable && it.text.toString() == "00:00" })
                assertTrue(fields.any { it.isClickable && it.text.toString() == "23:59" })
                val export = fields.single {
                    it.contentDescription?.toString() == "Messwerte als CSV exportieren"
                }
                assertTrue(export.isClickable && export.isFocusable)
                assertTrue(export.text.contains("CSV exportieren"))
                assertTrue(labels.any {
                    it.contains(ZoneId.systemDefault().id) && it.contains("Endminute inklusive")
                })
            }
        }
    }
}
