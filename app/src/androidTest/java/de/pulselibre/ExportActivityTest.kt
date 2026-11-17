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
import java.time.LocalTime
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
                assertTrue("Daten title missing", labels.contains("Daten"))
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
                listOf(
                    "Lokales Datenbankbackup erstellen",
                    "Datenbank aus einem Backup wiederherstellen",
                    "Alle lokalen Datenbankeinträge löschen"
                ).forEach { description ->
                    val action = fields.single { it.contentDescription?.toString() == description }
                    assertTrue("$description must be an accessible action", action.isClickable && action.isFocusable)
                }
                assertTrue(labels.any { it.contains("unverschlüsselt") })
                assertTrue(labels.any {
                    it.contains(ZoneId.systemDefault().id) && it.contains("Endminute inklusive")
                })
            }
        }
    }

    @Test fun exportRangeAndPendingFileWindowSurviveActivityRecreation() {
        ActivityScenario.launch(ExportActivity::class.java).use { scenario ->
            val from = LocalDate.of(2026, 2, 3)
            val through = LocalDate.of(2026, 2, 9)
            val fromTime = LocalTime.of(7, 15)
            val throughTime = LocalTime.of(21, 45)
            val pending = ExportWindow(1_770_100_000_000L, 1_770_200_000_000L)
            scenario.onActivity { activity ->
                setPrivateField(activity, "from", from)
                setPrivateField(activity, "through", through)
                setPrivateField(activity, "fromTime", fromTime)
                setPrivateField(activity, "throughTime", throughTime)
                setPrivateField(activity, "pendingWindow", pending)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals(from, privateField(activity, "from"))
                assertEquals(through, privateField(activity, "through"))
                assertEquals(fromTime, privateField(activity, "fromTime"))
                assertEquals(throughTime, privateField(activity, "throughTime"))
                assertEquals(pending, privateField(activity, "pendingWindow"))
            }
        }
    }

    private fun setPrivateField(target: Any, name: String, value: Any?) {
        target.javaClass.getDeclaredField(name).apply {
            isAccessible = true
            set(target, value)
        }
    }

    private fun privateField(target: Any, name: String): Any? =
        target.javaClass.getDeclaredField(name).let {
            it.isAccessible = true
            it.get(target)
        }
}
