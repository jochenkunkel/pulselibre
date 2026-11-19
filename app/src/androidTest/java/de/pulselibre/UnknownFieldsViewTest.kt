package de.pulselibre

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** #204 phase B: hint is hidden without data, shows the headline with data and opens a read-only list. */
@RunWith(AndroidJUnit4::class)
class UnknownFieldsViewTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun all(v: View): List<View> =
        listOf(v) + ((v as? ViewGroup)?.let { g -> (0 until g.childCount).flatMap { all(g.getChildAt(it)) } } ?: emptyList())

    private fun field(number: Int) = StoredFitUnmappedField(
        "c".repeat(64), 20, StoredFitFieldKind.STANDARD, number, 134, 4, null, "FIELD_NOT_DECODED_BY_APP", 1, 7, null, null, "d", "p",
        FitFieldInventoryStatus.UNREVIEWED
    )

    @Test fun hiddenWhenNothingIsStoredAndOpensListWhenFieldsExist() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var view: UnknownFieldsView? = null
            scenario.onActivity { activity ->
                val v = UnknownFieldsView(activity)
                (activity.findViewById<ViewGroup>(android.R.id.content)).addView(v)
                v.show(UnknownFieldsSummary.summarize(emptyList()))
                assertEquals(View.GONE, v.visibility)
                v.show(UnknownFieldsSummary.summarize(listOf(field(85), field(86))))
                assertEquals(View.VISIBLE, v.visibility)
                val texts = all(v).filterIsInstance<TextView>().map { it.text.toString() }
                assertTrue(texts.contains("Unbekannte Datenfelder entdeckt (2 Typen)"))
                assertTrue(v.contentDescription.toString().contains("Tippen für Details"))
                view = v
                v.performClick()
            }
            val dialog = DialogTestSupport.awaitDialog("Unbekannte Datenfelder entdeckt (2 Typen)")
            // grouped by message number, collapsed until tapped (#204 follow-up)
            assertTrue(DialogTestSupport.containsText(dialog, "FIT-Nachricht 20 · 2 Feldtypen · 14 Meldungen"))
            assertFalse(DialogTestSupport.containsText(dialog, "FIT-Nachricht 20 · Feld 85"))
            DialogTestSupport.clickText(dialog, "FIT-Nachricht 20 · 2 Feldtypen")
            assertTrue(DialogTestSupport.containsText(dialog, "FIT-Nachricht 20 · Feld 85"))
            assertTrue(DialogTestSupport.containsText(dialog, "7 Meldungen in 1 Datei"))
            assertTrue(DialogTestSupport.containsText(dialog, "ungeprüft"))
        }
    }
}
