package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

class MetricInfoIconMappingTest {
    @Test fun knownMarkersMapToTheirIcons() {
        assertEquals(PlIcon.INFO_RANGE, MetricInfoIconMapping.forSymbol("▬"))
        assertEquals(PlIcon.INFO_BAND, MetricInfoIconMapping.forSymbol("▰"))
        assertEquals(PlIcon.INFO_SOURCE, MetricInfoIconMapping.forSymbol("↗"))
        assertEquals(PlIcon.INFO_TIME, MetricInfoIconMapping.forSymbol("◷"))
        assertEquals(PlIcon.WARNING, MetricInfoIconMapping.forSymbol("⚠"))
        assertEquals(PlIcon.HOURGLASS, MetricInfoIconMapping.forSymbol("⏳"))
        assertEquals(PlIcon.SIGNAL, MetricInfoIconMapping.forSymbol("●"))
        assertEquals(PlIcon.GENERIC_METRIC, MetricInfoIconMapping.forSymbol("■"))
        assertEquals(PlIcon.INFO_WINDOW, MetricInfoIconMapping.forSymbol("▱"))
    }

    @Test fun unknownOrEmptyMarkerFallsBackToInfo() {
        assertEquals(PlIcon.INFO, MetricInfoIconMapping.forSymbol("i"))
        assertEquals(PlIcon.INFO, MetricInfoIconMapping.forSymbol(""))
        assertEquals(PlIcon.INFO, MetricInfoIconMapping.forSymbol("?"))
    }
}
