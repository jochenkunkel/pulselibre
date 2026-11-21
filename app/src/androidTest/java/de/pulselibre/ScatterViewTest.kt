package de.pulselibre

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScatterViewTest {
    private fun render(points: List<Pair<Double, Double>>): Pair<ScatterView, Bitmap> {
        val ctx = AppLanguageSettings.wrap(ApplicationProvider.getApplicationContext<Context>()).let { androidx.appcompat.view.ContextThemeWrapper(it, androidx.appcompat.R.style.Theme_AppCompat) }
        val v = ScatterView(ctx, points, "x", "y", Color.BLACK, Color.RED, Color.GRAY)
        v.measure(View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED)
        v.layout(0, 0, v.measuredWidth, v.measuredHeight)
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bmp))
        return v to bmp
    }

    @Test fun drawsPointsAndDescribesRangeAndCount() {
        val (v, bmp) = render((0 until 40).map { it * 100.0 to 55.0 + it % 7 })
        assertTrue(v.contentDescription.contains("40 Wertepaaren"))
        var red = 0
        for (x in 0 until bmp.width step 2) for (y in 0 until bmp.height step 2) if (Color.red(bmp.getPixel(x, y)) > 200 && Color.green(bmp.getPixel(x, y)) < 120) red++
        assertTrue("Punkte müssen sichtbar sein", red > 20)
    }

    @Test fun identicalValuesDoNotCrashOrDivideByZero() {
        val (_, bmp) = render(List(5) { 100.0 to 60.0 })
        assertEquals(800, bmp.width)
    }
}
