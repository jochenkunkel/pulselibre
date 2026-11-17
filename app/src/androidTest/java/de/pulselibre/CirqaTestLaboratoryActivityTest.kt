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
class CirqaTestLaboratoryActivityTest {
    @Test fun labClearlyBlocksHistoryAndExposesOnlyOfflineOrExistingTests() {
        ActivityScenario.launch(CirqaTestLaboratoryActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val views = mutableListOf<View>()
                fun visit(view: View) {
                    views += view
                    if (view is ViewGroup) {
                        for (index in 0 until view.childCount) visit(view.getChildAt(index))
                    }
                }
                visit(activity.findViewById(android.R.id.content))
                val labels = views.filterIsInstance<TextView>().map { it.text.toString() }
                assertTrue(labels.any { it.contains("CIRQA-Testlabor") })
                assertTrue(labels.any { it.contains("Historischer CIRQA-Download bleibt gesperrt") })
                assertTrue(labels.any { it.contains("GFDI 5007") })
                assertTrue(labels.any { it.contains("07 00 8F 13 03 C9 1A") })
                assertTrue(labels.any { it.contains("CIRQA-GFDI-Antworttyp") && it.contains("nicht bestätigt") })
                assertTrue(labels.any { it.contains("Gesperrt") && it.contains("Unbewiesen") })
                assertTrue(labels.any { it.contains("Offline-FIT") || it.contains("FIT-Datei") })
                assertTrue(views.filterIsInstance<TextView>().none {
                    it.text.toString().contains("Dateiliste anfragen") ||
                        it.text.toString().contains("Datei herunterladen") ||
                        it.text.toString().contains("0x8A39")
                })
                assertTrue(views.filterIsInstance<TextView>().any {
                    it.isClickable && it.isFocusable &&
                        it.text.toString().contains("FIT-Datei auswählen")
                })
                val oneShotTest = views.filterIsInstance<Button>().single {
                    it.text.toString().contains("5007-Test") || it.text.toString().contains("5007-Tests")
                }
                val consumed = CirqaLegacy5007AttemptGate.consumedAttempts(
                    activity.getSharedPreferences("cirqa_test_laboratory", 0)
                        .getInt("legacy5007TestAttemptCount", 0),
                    activity.getSharedPreferences("cirqa_test_laboratory", 0)
                        .getBoolean("legacy5007TestAttempted", false)
                )
                assertEquals(consumed < CirqaLegacy5007AttemptGate.MAX_AUTHORIZED_ATTEMPTS, oneShotTest.isEnabled)
                assertTrue(oneShotTest.isClickable)
            }
        }
    }
}
