package com.rr.numio.launcher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Readable text in every Ink page: WCAG contrast of 4.5:1 for body text (ink, labels,
 * secondary text, amber text) and for text on amber. The accent amber is for dots and
 * strokes only, so it is not checked as text.
 */
class InkContrastTest {

    private fun ratio(a: Color, b: Color): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return maxOf(la, lb) / minOf(la, lb)
    }

    private val pages = mapOf(
        "Light Ink, warm paper" to paperColors(dark = false, warm = true),
        "Light Ink, Numio paper" to paperColors(dark = false, warm = false),
        "Dark Ink" to paperColors(dark = true),
    )

    @Test fun textOnEveryPageIsReadable() {
        for ((name, c) in pages) {
            for ((what, fg) in listOf("ink" to c.ink, "soft text" to c.inkSoft, "amber text" to c.goldText)) {
                val r = ratio(fg, c.background)
                assertTrue("$name: $what is only ${"%.1f".format(r)}:1", r >= 4.5f)
            }
            // menus and dialogs use the card colour
            assertTrue("$name: ink on cards", ratio(c.ink, c.card) >= 4.5f)
            assertTrue("$name: text on amber", ratio(c.onGold, c.gold) >= 4.5f)
        }
    }
}
