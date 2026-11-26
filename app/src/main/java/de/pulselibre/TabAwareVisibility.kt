package de.pulselibre

import android.view.View

/**
 * A view on a tab that is only shown while it has content (sync progress, unknown-field hint). The tab switch used to
 * set every view of the Gerät tab VISIBLE, which drew the idle progress view as an empty card (S10, 26.11.2026); and a
 * view that made itself visible during a download appeared on whatever tab was open. Visible = tab shown AND content.
 */
internal interface TabAwareVisibility {
    var tabShown: Boolean
    val hasContent: Boolean

    companion object {
        fun visibility(tabShown: Boolean, hasContent: Boolean) = if (tabShown && hasContent) View.VISIBLE else View.GONE
    }
}
