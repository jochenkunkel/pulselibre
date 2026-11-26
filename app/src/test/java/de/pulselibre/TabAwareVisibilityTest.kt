package de.pulselibre

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Test

/** S10 26.11.2026: an idle sync-progress card was drawn empty on the Gerät tab. Visible only with tab AND content. */
class TabAwareVisibilityTest {
    @Test fun visibleOnlyWhenTabShownAndContentPresent() {
        assertEquals(View.VISIBLE, TabAwareVisibility.visibility(tabShown = true, hasContent = true))
        assertEquals(View.GONE, TabAwareVisibility.visibility(tabShown = true, hasContent = false)) // the empty card
        assertEquals(View.GONE, TabAwareVisibility.visibility(tabShown = false, hasContent = true)) // other tab open
        assertEquals(View.GONE, TabAwareVisibility.visibility(tabShown = false, hasContent = false))
    }
}
