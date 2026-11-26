package de.pulselibre

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScatterViewTest {
    @Test fun emptyListDoesNotCrash() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = ScatterView(ApplicationProvider.getApplicationContext<Context>(), emptyList(), "x", "y", 0, 0, 0)
            view.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED)
            view.layout(0, 0, 400, view.measuredHeight)
            view.draw(Canvas(Bitmap.createBitmap(400, view.measuredHeight, Bitmap.Config.ARGB_8888)))
            assertNull(view.contentDescription)
        }
    }
}
