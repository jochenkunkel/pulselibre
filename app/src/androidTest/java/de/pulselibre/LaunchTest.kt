package de.pulselibre

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import android.view.View
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaunchTest {
    @Test fun launchesAndShowsScanButton() {
        ActivityScenario.launch(MainActivity::class.java).use {
            it.onActivity { activity ->
                val root = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
                assertFalse(root.childCount == 0)
            }
        }
    }
    @Test fun primaryNavigationKeepsReadableLabelsAndAddsMatchingIcons() {
        ActivityScenario.launch(MainActivity::class.java).use {
            it.onActivity { activity ->
                val root = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
                listOf("Übersicht", "Verlauf", "Analysen", "Gerät", "Daten").forEach { label ->
                    val item = root.findViewWithTag<View>("navigation_item_$label")
                    assertNotNull(item)
                    assertEquals(label, item.contentDescription)
                    assertNotNull(root.findViewWithTag<View>("navigation_icon_$label"))
                }
            }
        }
    }

    @Test fun selectedMainTabSurvivesRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                it.findViewById<View>(android.R.id.content)
                    .findViewWithTag<View>("navigation_item_Verlauf").performClick()
            }
            scenario.recreate()
            scenario.onActivity {
                val tab = it.findViewById<View>(android.R.id.content)
                    .findViewWithTag<View>("navigation_item_Verlauf")
                org.junit.Assert.assertTrue(tab.isSelected)
            }
        }
    }

    @Test fun noInternetPermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(android.content.pm.PackageManager.PERMISSION_DENIED,
            context.checkSelfPermission(android.Manifest.permission.INTERNET))
    }
}
