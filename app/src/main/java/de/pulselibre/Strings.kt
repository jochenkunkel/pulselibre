package de.pulselibre

import android.content.Context

/**
 * Resource lookup that does not need an Android [Context], so text-building logic stays unit-testable: production code passes
 * [Strings.of], unit tests pass a lookup over the real German resource file (see GermanStrings in the test sources).
 */
internal fun interface Strings {
    fun get(id: Int, vararg args: Any): String

    companion object {
        fun of(context: Context) = Strings { id, args -> context.getString(id, *args) }
    }
}
