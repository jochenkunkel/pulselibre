package de.pulselibre

import android.content.Context
import android.widget.ImageView

/** Compatibility view: old information kinds now resolve exclusively to PulsLibre SVG resources. */
internal class DashboardInfoIconView(context: Context, kind: Kind, tint: Int) : ImageView(context) {
    enum class Kind { TIME, RANGE, WINDOW, SOURCE, SCALE, LIMIT, HOME, HISTORY, DEVICES, LAB, DATA }

    init {
        setImageDrawable(PlIcon.drawable(context, iconFor(kind), 24, tint))
        scaleType = ScaleType.FIT_CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    companion object {
        fun iconFor(kind: Kind): PlIcon = when (kind) {
            Kind.TIME -> PlIcon.INFO_TIME
            Kind.RANGE -> PlIcon.INFO_BAND
            Kind.WINDOW -> PlIcon.INFO_WINDOW
            Kind.SOURCE -> PlIcon.INFO_SOURCE
            Kind.SCALE -> PlIcon.INFO_RANGE
            Kind.LIMIT -> PlIcon.WARNING
            Kind.HOME -> PlIcon.OVERVIEW
            Kind.HISTORY -> PlIcon.HISTORY
            Kind.DEVICES -> PlIcon.DEVICE
            Kind.LAB -> PlIcon.TESTLAB
            Kind.DATA -> PlIcon.DATA
        }
    }
}
