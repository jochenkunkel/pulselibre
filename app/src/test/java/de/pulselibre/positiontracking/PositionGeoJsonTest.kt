package de.pulselibre.positiontracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic coordinates only; no real positions in tests. */
class PositionGeoJsonTest {
    private val session = PositionSession(7, 1_000L, 9_000L, null)

    @Test fun lineStringUsesLonLatOrderAndKeepsAccuracy() {
        val json = PositionGeoJson.export(session, listOf(
            PositionPoint(2_000L, 10.5, 20.25, 4.5f, "gps"),
            PositionPoint(1_500L, 10.0, 20.0, 35f, "network")
        ))
        assertTrue(json.startsWith("{\"type\":\"FeatureCollection\""))
        assertTrue(json.contains("\"type\":\"LineString\",\"coordinates\":[[20,10],[20.25,10.5]]"))
        assertTrue(json.contains("\"accuracyMeters\":35,\"provider\":\"network\""))
        assertTrue(json.contains("\"accuracyMeters\":4.5,\"provider\":\"gps\""))
        assertTrue(json.contains("\"points\":2"))
    }

    @Test fun zeroAndOnePoint() {
        assertEquals("{\"type\":\"FeatureCollection\",\"features\":[]}", PositionGeoJson.export(session, emptyList()))
        val one = PositionGeoJson.export(session, listOf(PositionPoint(2_000L, 1.0, 2.0)))
        assertTrue(one.contains("\"geometry\":{\"type\":\"Point\",\"coordinates\":[2,1]},\"properties\":{\"kind\":\"track\""))
        assertFalse(one.contains("LineString"))
        assertFalse(one.contains("accuracyMeters")) // never invented
    }

    @Test fun invalidPositionsAreDropped() {
        assertFalse(PositionPoint(1L, Double.NaN, 1.0).isValid())
        assertFalse(PositionPoint(1L, 91.0, 1.0).isValid())
        assertFalse(PositionPoint(1L, 0.0, 0.0).isValid())
        assertTrue(PositionPoint(1L, -33.9, 151.2).isValid())
        val json = PositionGeoJson.export(session, listOf(PositionPoint(2_000L, 1.0, 2.0), PositionPoint(3_000L, 0.0, 0.0)))
        assertTrue(json.contains("\"points\":1"))
    }
}
