package de.pulselibre

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Header mark: light theme gets a thin light edge (contrast against the violet banner), dark theme stays plain. */
@RunWith(AndroidJUnit4::class)
class PulseLogoViewTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun render(outline: Boolean): Bitmap {
        val view = PulseLogoView(context, outline)
        val size = (48 * context.resources.displayMetrics.density).toInt()
        view.measure(View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, size, size)
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
    }

    /** Brightness of the pixel at the middle of the left edge, 1 dp inside (where the outline is drawn). */
    private fun edgeBrightness(bitmap: Bitmap): Int {
        val x = (context.resources.displayMetrics.density * 0.9f).toInt().coerceAtLeast(1)
        val c = bitmap.getPixel(x, bitmap.height / 2)
        return (Color.red(c) + Color.green(c) + Color.blue(c)) / 3
    }

    @Test fun outlineMakesTheEdgeClearlyLighterThanTheVioletFill() {
        val plain = edgeBrightness(render(false))
        val outlined = edgeBrightness(render(true))
        assertTrue("plain=$plain outlined=$outlined", plain < 90)          // violet fill #5127C9 is dark
        assertTrue("plain=$plain outlined=$outlined", outlined > plain + 70) // visibly lighter edge
    }
}
