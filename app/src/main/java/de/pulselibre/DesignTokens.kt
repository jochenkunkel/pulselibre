package de.pulselibre

import android.graphics.Color

/** Colour tokens of the premium dashboard redesign (#124); values from the design brief, light variants derived. */
internal object DesignTokens {
    val BRAND_PURPLE = Color.parseColor("#5127C9")
    val MINT = Color.parseColor("#22E5C1")
    val GLOW = Color.parseColor("#B46AFF")

    fun headerColors(dark: Boolean) =
        if (dark) intArrayOf(Color.parseColor("#1B1030"), Color.parseColor("#24133C"), Color.parseColor("#101527"))
        else intArrayOf(Color.parseColor("#4B2CB0"), Color.parseColor("#5A33C7"), Color.parseColor("#3A2290"))

    fun heroColors(dark: Boolean) =
        if (dark) intArrayOf(Color.parseColor("#52276C"), Color.parseColor("#1B1935"))
        else intArrayOf(Color.parseColor("#F3ECFF"), Color.parseColor("#DCCFFA"))

    fun heroStroke(dark: Boolean) = if (dark) Color.parseColor("#6E3FD0") else Color.parseColor("#B79AF0")
    fun heroText(dark: Boolean) = if (dark) Color.WHITE else Color.parseColor("#231D3B")
    fun heroSecondary(dark: Boolean) = if (dark) Color.parseColor("#D6CBE7") else Color.parseColor("#5B5472")
    /** Value labels on the hero card (kept above 4.5:1 against the gradient). */
    fun heroAccent(dark: Boolean) = if (dark) Color.parseColor("#C9A8FF") else Color.parseColor("#5127C9")
}
