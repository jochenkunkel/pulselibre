package de.pulselibre

import de.pulselibre.FitnessOverview.State
import de.pulselibre.TrendAnalysis.Direction
import org.junit.Assert.*
import org.junit.Test

class FitnessOverviewTest {
    private fun t(d: Direction) = TrendAnalysis.Result(28,
        TrendAnalysis.Window(28, 20, 1.0), TrendAnalysis.Window(28, 20, 1.0), null, d, "")
    private fun fitness(a: Direction, r: Direction) = FitnessOverview.assess(t(a), t(r)).dimensions.first { it.name == "Fitnessentwicklung" }

    @Test fun noScoreAndRecoveryAlwaysNotAssessable() {
        val res = FitnessOverview.assess(t(Direction.UP), t(Direction.DOWN))
        assertEquals(3, res.dimensions.size)
        assertEquals(State.NOT_ASSESSABLE, res.dimensions.single { it.name == "Regeneration" }.state)
    }
    @Test fun consistentActivityUpRestingDownIsPositiveAndWorded() {
        val d = fitness(Direction.UP, Direction.DOWN)
        assertEquals(State.POSITIVE, d.state); assertTrue(d.sentence.contains("keine Diagnose"))
    }
    @Test fun fallingRestingPulseAloneIsNeverPositive() {
        assertEquals(State.NEUTRAL, fitness(Direction.UNKNOWN, Direction.DOWN).state)
        assertEquals(State.NEUTRAL, fitness(Direction.STABLE, Direction.DOWN).state)
    }
    @Test fun contradictoryTrendsAreMixed() {
        assertEquals(State.MIXED, fitness(Direction.DOWN, Direction.UP).state)
        assertEquals(State.MIXED, fitness(Direction.UP, Direction.UP).state)
    }
    @Test fun poorCoverageIsNotAssessable() {
        assertEquals(State.NOT_ASSESSABLE, fitness(Direction.UNKNOWN, Direction.UNKNOWN).state)
    }
    @Test fun activityDimensionNeverPositive() {
        for (d in Direction.entries) {
            val s = FitnessOverview.assess(t(d), t(Direction.UNKNOWN)).dimensions.single { it.name == "Aktivitätskonstanz" }.state
            assertNotEquals(State.POSITIVE, s)
        }
    }
}
