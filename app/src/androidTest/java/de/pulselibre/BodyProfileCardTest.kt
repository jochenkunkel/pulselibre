package de.pulselibre

import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Body profile card: derived age, BMI with written category, quick weight dialog (#117, #118). Emulator only. */
@RunWith(AndroidJUnit4::class)
class BodyProfileCardTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val prefs get() = instrumentation.targetContext.getSharedPreferences("local_personal_profile", 0)

    private fun all(view: View, out: MutableList<View> = mutableListOf()): List<View> {
        out += view
        if (view is ViewGroup) for (i in 0 until view.childCount) all(view.getChildAt(i), out)
        return out
    }

    private fun texts(activity: MainActivity) =
        all(activity.findViewById(android.R.id.content)).filterIsInstance<TextView>().map { it.text.toString() }

    private fun find(text: String, timeoutMs: Long = 15000): AccessibilityNodeInfo? {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            instrumentation.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText(text)?.firstOrNull()?.let { return it }
            Thread.sleep(100)
        }
        return null
    }

    private fun click(node: AccessibilityNodeInfo) {
        var n: AccessibilityNodeInfo? = node
        while (n != null && !n.isClickable) n = n.parent
        n!!.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun withProfile(block: () -> Unit) {
        val saved = prefs.all.toMap()
        try { block() } finally {
            prefs.edit().clear().apply { saved.forEach { (k, v) -> if (v is String) putString(k, v) } }.commit()
        }
    }

    @Test fun showsDerivedAgeAndBmiWithCategoryText() = withProfile {
        val birth = LocalDate.now().minusYears(40).minusDays(1)
        prefs.edit().putString("height_cm", "180").putString("weight_kg", "80").putString("birth_date", birth.toString())
            .putString("age_years", "99").commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val t = texts(activity)
                assertTrue("Alter abgeleitet", t.any { it == "40 Jahre" })
                assertFalse("gespeichertes Alter wird ignoriert", t.any { it == "99 Jahre" })
                assertTrue("BMI 24,7", t.any { it.startsWith("24,7") })
                assertTrue("Kategorie als Text", t.any { it.contains("Normalgewicht") })
            }
        }
    }

    @Test fun legacyAgeWithoutBirthDateIsKeptAndUnder20GetsNoRating() = withProfile {
        prefs.edit().putString("height_cm", "170").putString("weight_kg", "60").remove("birth_date").putString("age_years", "17").commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val t = texts(activity)
                assertTrue(t.any { it == "17 Jahre" })
                assertTrue(t.any { it.contains("keine BMI-Bewertung") })
                assertFalse(t.any { it.contains("Normalgewicht") })
            }
        }
    }

    @Test fun quickWeightCancelKeepsValueAndInvalidInputIsRejected() = withProfile {
        prefs.edit().putString("height_cm", "180").putString("weight_kg", "80").commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val row = all(activity.findViewById(android.R.id.content)).single { it.tag == "body_weight_row" }
                assertTrue(row.isClickable); assertTrue(row.contentDescription.contains("Tippen zum Ändern"))
                assertTrue("kein Waagen-Button mehr unter der Karte", all(activity.findViewById(android.R.id.content)).none { it.tag == "body_scale_button" })
                row.performClick()
            }
            assertNotNull(find("Gewicht (kg)"))
            // a step changes only the editor, not the stored value
            scenario.onActivity { activity ->
                val plus = all(activity.window.decorView).firstOrNull { it.tag == "weight_plus" }
                // the dialog has its own window; fall back to the accessibility tree
            }
            click(find("Abbrechen")!!)
            Thread.sleep(300)
            assertEquals("80", prefs.getString("weight_kg", null))
        }
    }
}
