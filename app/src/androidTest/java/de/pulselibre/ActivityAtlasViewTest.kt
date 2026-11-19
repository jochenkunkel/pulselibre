package de.pulselibre

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** #41 stage 1: the atlas view reports a summary and opens the day detail with honest wording. */
@RunWith(AndroidJUnit4::class)
class ActivityAtlasViewTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val today = LocalDate.of(2026, 11, 19)

    @Test fun viewDescribesCoverageAndMeasuresToTheCalendarHeight() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            var tapped: ActivityAtlas.Cell? = null
            val view = ActivityAtlasView(context) { tapped = it }
            val data = (1..20).associate { today.minusDays(it.toLong()) to 4000.0 + it * 100 }
            val model = ActivityAtlas.build(data, today, 28)
            view.show(model)
            view.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED)
            assertTrue(view.measuredHeight > 0)
            assertTrue(view.contentDescription.toString().contains("20 von 28 Tagen"))
            assertEquals(null, tapped)
        }
    }

    @Test fun trendCardShowsAtlasWithSegmentsAndNoDiagnosisWording() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val card = TrendCardView(context)
            val d = TrendCardView.Data(
                FitnessOverview.assess(TrendAnalysis.analyze(emptyMap(), today, 28, 500.0), TrendAnalysis.analyze(emptyMap(), today, 28, 1.0)),
                listOf(7, 28, 90).map { TrendAnalysis.analyze(emptyMap(), today, it, 1.0) },
                listOf(7, 28, 90).map { TrendAnalysis.analyze(emptyMap(), today, it, 500.0) },
                PairedCorrelation.analyze(emptyMap(), emptyMap(), 1), emptyList(),
                emptyMap(), (1..10).associate { today.minusDays(it.toLong()) to 5000.0 + it }, today
            )
            card.javaClass.getDeclaredMethod("render", TrendCardView.Data::class.java).apply { isAccessible = true }.invoke(card, d)
            fun all(v: View): List<View> = listOf(v) + ((v as? ViewGroup)?.let { g -> (0 until g.childCount).flatMap { all(g.getChildAt(it)) } } ?: emptyList())
            val views = all(card)
            assertTrue(views.any { it.tag == "activity_atlas" })
            assertTrue(views.any { it.tag == "atlas_window_28" } && views.any { it.tag == "atlas_window_7" } && views.any { it.tag == "atlas_window_90" })
            val summary = views.filterIsInstance<TextView>().first { it.tag == "activity_atlas_summary" }.text.toString()
            assertEquals("10 von 28 Tagen mit verifizierten Schrittwerten", summary)
            // switching to 90 days must report coverage honestly
            views.first { it.tag == "atlas_window_90" }.performClick()
            val summary90 = all(card).filterIsInstance<TextView>().first { it.tag == "activity_atlas_summary" }.text.toString()
            assertEquals("10 von 90 Tagen mit verifizierten Schrittwerten", summary90)
        }
    }

    private fun cardWith(rest: Map<LocalDate, Double>): TrendCardView {
        val card = TrendCardView(context)
        val d = TrendCardView.Data(
            FitnessOverview.assess(TrendAnalysis.analyze(emptyMap(), today, 28, 500.0), TrendAnalysis.analyze(rest, today, 28, 1.0)),
            listOf(7, 28, 90).map { TrendAnalysis.analyze(rest, today, it, 1.0) },
            listOf(7, 28, 90).map { TrendAnalysis.analyze(emptyMap(), today, it, 500.0) },
            PairedCorrelation.analyze(emptyMap(), emptyMap(), 1), emptyList(), rest, emptyMap(), today
        )
        card.javaClass.getDeclaredMethod("render", TrendCardView.Data::class.java).apply { isAccessible = true }.invoke(card, d)
        return card
    }
    private fun all(v: View): List<View> = listOf(v) + ((v as? ViewGroup)?.let { g -> (0 until g.childCount).flatMap { all(g.getChildAt(it)) } } ?: emptyList())

    @Test fun heroWithoutRestingPulseShowsNoFictionalValue() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val views = all(cardWith(emptyMap()))
            assertEquals("–", views.filterIsInstance<TextView>().first { it.tag == "resting_hero_value" }.text.toString())
            assertTrue(views.filterIsInstance<TextView>().first { it.tag == "resting_hero_message" }.text.toString()
                .startsWith("Noch keine verifizierten Ruhepulswerte"))
            assertTrue(views.any { it.tag == "resting_pulse_chart" })
        }
    }

    @Test fun heroShowsMedianAndNeutralDeltaAndFollowsTheSharedWindow() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val rest = (1..56).associate { today.minusDays(it.toLong()) to if (it <= 28) 72.0 else 76.0 }
            val card = cardWith(rest)
            val views = all(card)
            assertEquals("72 bpm", views.filterIsInstance<TextView>().first { it.tag == "resting_hero_value" }.text.toString())
            val delta = views.filterIsInstance<TextView>().first { it.tag == "resting_hero_delta" }
            assertEquals("−4 bpm gegenüber vorherigen 28 Tagen", delta.text.toString())
            views.first { it.tag == "atlas_window_7" }.performClick()
            val delta7 = all(card).filterIsInstance<TextView>().first { it.tag == "resting_hero_delta" }.text.toString()
            assertTrue(delta7.endsWith("vorherigen 7 Tagen"))
        }
    }

    @Test fun coverageIsShownSeparatelyForRestingPulseAndStepsAndFollowsTheWindow() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val rest = (1..5).associate { today.minusDays(it.toLong()) to 60.0 }
            val card = cardWith(rest)
            val views = all(card)
            val box = views.first { it.tag == "data_coverage_box" }
            val texts = all(box).filterIsInstance<TextView>().map { it.text.toString() }
            assertTrue(texts.contains("Ruhepuls: 5 von 28 Tagen"))
            assertTrue(texts.contains("Schritte: 0 von 28 Tagen"))
            assertTrue(texts.any { it.contains("mindestens 14") })
            views.first { it.tag == "atlas_window_7" }.performClick()
            val texts7 = all(card).first { it.tag == "data_coverage_box" }.let { b -> all(b).filterIsInstance<TextView>().map { it.text.toString() } }
            assertTrue(texts7.contains("Ruhepuls: 5 von 7 Tagen"))
        }
    }
}
