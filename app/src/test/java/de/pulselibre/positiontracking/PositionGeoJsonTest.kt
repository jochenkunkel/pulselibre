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

    @Test fun summaryAndNearestHealthValues() {
        // two points 0.001° latitude apart = about 111 m, 60 s apart
        val pts = listOf(PositionPoint(1_000L, 10.0, 20.0, 5f, "gps"), PositionPoint(61_000L, 10.001, 20.0, 5f, "gps"))
        val hr = listOf(1_500L to 100.0, 59_000L to 120.0)
        val json = PositionGeoJson.export(session, pts, mapOf("heartRateBpm" to hr))
        assertTrue(json, json.contains("\"durationSeconds\":60"))
        assertTrue(json, Regex("\"distanceMeters\":111\\.").containsMatchIn(json))
        assertTrue(json.contains("\"averageHeartRateBpm\":110"))
        assertTrue(json.contains("\"maxHeartRateBpm\":120"))
        assertTrue(json.contains("\"heartRateBpm\":100"))
        assertTrue(json.contains("\"heartRateBpm\":120"))
    }

    @Test fun healthFurtherThanTenSecondsIsNotAttached() {
        assertEquals(null, PositionGeoJson.nearest(listOf(0L to 80.0), 20_000L))
        assertEquals(80.0, PositionGeoJson.nearest(listOf(0L to 80.0, 50_000L to 90.0), 9_000L)!!, 0.0)
        assertEquals(90.0, PositionGeoJson.nearest(listOf(0L to 80.0, 50_000L to 90.0), 45_000L)!!, 0.0)
    }
}
