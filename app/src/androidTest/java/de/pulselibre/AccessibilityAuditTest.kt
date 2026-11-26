package de.pulselibre

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automatic accessibility check of every main screen (dependency-free; the project keeps its test dependencies minimal).
 * Rules for every visible, enabled, clickable view:
 *  1. TalkBack has something to say: a content description, its own text, or readable text of its children.
 *  2. The touch target is at least 48 × 48 dp (Android accessibility guideline).
 *  3. A clickable image has a content description.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityAuditTest {
    private val tabs = listOf("Übersicht", "Verlauf", "Analysen", "Gerät")

    private fun all(v: View): List<View> =
        listOf(v) + ((v as? ViewGroup)?.let { g -> (0 until g.childCount).flatMap { all(g.getChildAt(it)) } } ?: emptyList())

    private fun visible(v: View): Boolean {
        if (!v.isShown || v.width == 0 || v.height == 0) return false
        return v.getGlobalVisibleRect(Rect())
    }

    /** What TalkBack would read for [v]: description, own text, or the texts of children it merges. */
    private fun spoken(v: View): String {
        v.contentDescription?.takeIf { it.isNotBlank() }?.let { return it.toString() }
        (v as? TextView)?.text?.takeIf { it.isNotBlank() }?.let { return it.toString() }
        if (v is ViewGroup) return all(v).drop(1).filter { it.importantForAccessibility != View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS }
            .mapNotNull { c -> c.contentDescription?.takeIf { it.isNotBlank() } ?: (c as? TextView)?.text?.takeIf { it.isNotBlank() } }
            .joinToString(" ")
        return ""
    }

    private fun describe(v: View): String {
        val tag = v.tag?.toString()?.let { " tag=$it" } ?: ""
        val text = spoken(v).take(40).replace("\n", " ")
        return "${v.javaClass.simpleName}$tag \"$text\" ${v.width}x${v.height}px"
    }

    private fun audit(root: View, where: String, density: Float): List<String> {
        val min = (48 * density).toInt() - 1 // rounding of dp → px
        val problems = mutableListOf<String>()
        all(root).filter { it.isClickable && it.isEnabled && visible(it) }.forEach { v ->
            if (spoken(v).isBlank()) problems += "$where: keine Beschriftung – ${describe(v)}"
            if (v is ImageView && v.contentDescription.isNullOrBlank()) problems += "$where: Bild ohne Beschreibung – ${describe(v)}"
            if (v.width < min || v.height < min) problems += "$where: Touchfläche < 48 dp – ${describe(v)}"
        }
        return problems
    }

    private fun settle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(1_200)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @Test fun mainScreensAreUsableWithTalkBackAndTouch() {
        val problems = mutableListOf<String>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            tabs.forEach { id ->
                scenario.onActivity { it.findViewById<View>(android.R.id.content).findViewWithTag<View>("navigation_item_$id").performClick() }
                settle()
                scenario.onActivity {
                    problems += audit(it.findViewById(android.R.id.content), id, it.resources.displayMetrics.density)
                }
            }
        }
        ActivityScenario.launch(ExportActivity::class.java).use { scenario ->
            settle()
            scenario.onActivity { problems += audit(it.findViewById(android.R.id.content), "Daten", it.resources.displayMetrics.density) }
        }
        assertTrue("${problems.size} Barrierefreiheitsprobleme:\n" + problems.distinct().joinToString("\n"), problems.isEmpty())
    }
}
