package de.pulselibre

import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** The shared tab strip must exist, label all five areas and mark the current one (#64). */
@RunWith(AndroidJUnit4::class)
class MainNavigationViewTest {
    private val labels = listOf("Übersicht", "Verlauf", "Gerät", "Testlabor", "Daten")

    private fun all(view: View, out: MutableList<View> = mutableListOf()): List<View> {
        out += view
        if (view is ViewGroup) for (i in 0 until view.childCount) all(view.getChildAt(i), out)
        return out
    }

    @Test fun exportScreenShowsAllFiveTabsWithDataSelected() {
        ActivityScenario.launch(ExportActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val views = all(activity.findViewById(android.R.id.content))
                labels.forEach { label ->
                    val item = views.singleOrNull { it.tag == "navigation_item_$label" }
                    assertNotNull("Tab $label fehlt", item)
                    assertEquals(label, item!!.contentDescription)
                    assertTrue(item.isFocusable)
                }
                assertTrue(views.single { it.tag == "navigation_item_Daten" }.isSelected)
                labels.filter { it != "Daten" }.forEach {
                    assertFalse(views.single { v -> v.tag == "navigation_item_$it" }.isSelected)
                }
            }
        }
    }

    @Test fun mainScreenShowsAllFiveTabsWithOverviewSelectedAndHistoryClickSwitches() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val views = all(activity.findViewById(android.R.id.content))
                labels.forEach { assertNotNull("Tab $it fehlt", views.singleOrNull { v -> v.tag == "navigation_item_$it" }) }
                assertTrue(views.single { it.tag == "navigation_item_Übersicht" }.isSelected)
                views.single { it.tag == "navigation_item_Verlauf" }.performClick()
            }
            scenario.onActivity { activity ->
                val views = all(activity.findViewById(android.R.id.content))
                assertTrue(views.single { it.tag == "navigation_item_Verlauf" }.isSelected)
                assertFalse(views.single { it.tag == "navigation_item_Übersicht" }.isSelected)
            }
        }
    }
}
