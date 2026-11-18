package de.pulselibre

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.test.platform.app.InstrumentationRegistry

/** Deterministic access to the dialogs the app just showed (see [DialogProbe]); no UiAutomation involved. */
internal object DialogTestSupport {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private fun texts(v: View, out: MutableList<Pair<View, String>> = mutableListOf()): List<Pair<View, String>> {
        (v as? TextView)?.let { out += v to it.text.toString() }
        v.contentDescription?.let { if (v !is TextView) out += v to it.toString() }
        if (v is ViewGroup) for (i in 0 until v.childCount) texts(v.getChildAt(i), out)
        return out
    }

    /** Waits until the current dialog shows [text] (substring) and returns it. */
    fun awaitDialog(text: String, timeoutMs: Long = 20000): AlertDialog {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            var hit: AlertDialog? = null
            instrumentation.runOnMainSync {
                val d = DialogProbe.current
                if (d != null && d.isShowing && texts(d.window!!.decorView).any { it.second.contains(text) }) hit = d
            }
            hit?.let { return it }
            Thread.sleep(100)
        }
        throw AssertionError("Dialog mit '$text' nicht gefunden")
    }

    fun containsText(d: AlertDialog, text: String): Boolean {
        var r = false
        instrumentation.runOnMainSync { r = texts(d.window!!.decorView).any { it.second.contains(text) } }
        return r
    }

    fun clickText(d: AlertDialog, text: String) {
        var done = false
        instrumentation.runOnMainSync {
            texts(d.window!!.decorView).firstOrNull { it.second.contains(text) }?.first?.let { v ->
                var t: View? = v
                while (t != null && !t.isClickable) t = t.parent as? View
                done = t?.performClick() == true
            }
        }
        if (!done) throw AssertionError("'$text' nicht klickbar/gefunden")
    }

    fun clickButton(d: AlertDialog, which: Int) {
        instrumentation.runOnMainSync { d.getButton(which).performClick() }
    }
}
