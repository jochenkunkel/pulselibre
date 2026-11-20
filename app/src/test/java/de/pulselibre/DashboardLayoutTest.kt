package de.pulselibre

import de.pulselibre.DashboardLayout.Entry
import org.junit.Assert.*
import org.junit.Test

class DashboardLayoutTest {
    private val details = listOf("Stress (Geräteskala)", "Atemfrequenz")
    private val keys = DashboardLayout.defaultKeys(details)
    private val default = keys.map { Entry(it) }

    @Test fun defaultLayoutIsRecognisedAndKeepsBuiltInOrder() {
        assertTrue(DashboardLayout.isDefault(default, keys))
        assertEquals(keys, DashboardLayout.displayOrder(default).map { it.key })
    }

    @Test fun hidingAndFavouritesChangeTheDisplayOrderOnly() {
        var e = DashboardLayout.toggleVisible(default, DashboardLayout.SLEEP)
        e = DashboardLayout.toggleFavorite(e, "Atemfrequenz")
        assertFalse(DashboardLayout.isDefault(e, keys))
        val order = DashboardLayout.displayOrder(e).map { it.key }
        assertEquals("Atemfrequenz", order.first())
        assertFalse(DashboardLayout.SLEEP in order)
        assertEquals(keys.size, e.size)   // hidden tiles stay in the list
    }

    @Test fun moveStopsAtBothEnds() {
        assertEquals(default, DashboardLayout.moveUp(default, DashboardLayout.HEART))
        assertEquals(default, DashboardLayout.moveDown(default, details.last()))
        val moved = DashboardLayout.moveDown(default, DashboardLayout.HEART)
        assertEquals(DashboardLayout.SPO2, moved[0].key)
        assertEquals(DashboardLayout.HEART, moved[1].key)
        assertEquals(default, DashboardLayout.moveUp(moved, DashboardLayout.HEART))
        assertEquals(default, DashboardLayout.moveUp(default, "gibt es nicht"))
    }

    @Test fun resolveAppendsNewTilesAndDropsVanishedOnes() {
        val stored = listOf(Entry("Atemfrequenz", visible = false), Entry("Verschwunden"), Entry(DashboardLayout.HEART, favorite = true))
        val r = DashboardLayout.resolve(stored, keys)
        assertEquals(listOf("Atemfrequenz", DashboardLayout.HEART), r.take(2).map { it.key })
        assertFalse(r[0].visible); assertTrue(r[1].favorite)
        assertEquals(keys.toSet(), r.map { it.key }.toSet())
        assertTrue(r.drop(2).all { it.visible && !it.favorite })
        assertTrue(r.none { it.key == "Verschwunden" })
    }

    @Test fun rowsPairHalfWidthTilesAndFullWidthTilesStandAlone() {
        val order = listOf(DashboardLayout.HEART, DashboardLayout.SLEEP, DashboardLayout.SPO2, "A", "B", DashboardLayout.STEPS, "C")
            .map { Entry(it) }
        assertEquals(listOf(listOf(DashboardLayout.HEART), listOf(DashboardLayout.SLEEP), listOf(DashboardLayout.SPO2, "A"),
            listOf("B"), listOf(DashboardLayout.STEPS), listOf("C")), DashboardLayout.rows(order))
        assertTrue(DashboardLayout.rows(emptyList()).isEmpty())
    }

    @Test fun encodeDecodeRoundTripAndDamagedInputFallsBackSafely() {
        val e = listOf(Entry("Sauerstoffsättigung · SpO₂", visible = false, favorite = true), Entry("Schritte (Tageszähler)"))
        assertEquals(e, DashboardLayout.decode(DashboardLayout.encode(e)))
        assertTrue(DashboardLayout.decode(null).isEmpty())
        assertTrue(DashboardLayout.decode("müll").isEmpty())
        assertEquals(1, DashboardLayout.decode(DashboardLayout.encode(e) + "\u001fkaputt\u001e2\u001e0").size - 1)
        // an empty/invalid stored layout resolves to the built-in layout
        assertTrue(DashboardLayout.isDefault(DashboardLayout.resolve(DashboardLayout.decode("müll"), keys), keys))
    }
}
