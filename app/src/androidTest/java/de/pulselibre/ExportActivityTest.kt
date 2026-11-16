package de.pulselibre

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportActivityTest {
    @Test fun exportScreenShowsDateRangeAndExportButton() {
        ActivityScenario.launch(ExportActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                val labels = mutableListOf<String>()
                fun visit(view: View) {
                    if (view is TextView) labels += view.text.toString()
                    if (view is ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i))
                }
                visit(root)
                assertTrue(labels.any { it.contains("Datenexport") })
                assertTrue(labels.any { it.startsWith("Von:") })
                assertTrue(labels.any { it.startsWith("Bis einschließlich:") })
                assertTrue(labels.any { it == "CSV exportieren" })
                assertTrue(labels.any { it.contains("Zeitzone:") })
            }
        }
    }
}
