package com.rr.numio.launcher.ui

import androidx.compose.ui.graphics.Color
import com.rr.numio.launcher.data.Background
import com.rr.numio.launcher.data.PortalRules
import com.rr.numio.launcher.sketch.SketchIcons
import com.rr.numio.launcher.sketch.SketchPalette
import com.rr.numio.launcher.sketch.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Light Ink and Dark Ink use Numio's official colours; Sol and the logo are untouched. */
class SketchbookThemeTest {

    @Test fun lightInkOnWarmOrNumioPaper() {
        val warm = paperColors(dark = false)                 // warm is the default
        assertEquals(Color(0xFFEFE6D3), warm.background)
        assertEquals(Color(0xFFF6F4EE), paperColors(dark = false, warm = false).background)
        for (c in listOf(warm, paperColors(dark = false, warm = false))) {
            assertEquals(Color(0xFF191815), c.ink)
            assertEquals(Color(0xFFE9A800), c.gold)          // accents on paper
            assertEquals(Color(0xFF7A5600), c.goldText)      // amber text on paper
            assertEquals(Color(0xFF1A1400), c.onGold)        // text on amber
            assertTrue(c.sketchbook && !c.pageDark)
        }
        assertEquals(SketchPalette.LIGHT_WARM, warm.icons)
        assertEquals(SketchPalette.LIGHT_BRAND, paperColors(dark = false, warm = false).icons)
    }

    @Test fun darkInk() {
        val d = paperColors(dark = true)
        assertEquals(Color(0xFF0F0F0E), d.background)
        assertEquals(Color(0xFFF1EEE6), d.ink)
        assertEquals(Color(0xFFFFC21A), d.gold)
        assertEquals(Color(0xFF1A1400), d.onGold)
        assertTrue(d.sketchbook && d.pageDark)
        assertEquals(SketchPalette.DARK, d.icons)
        assertEquals(d, homeColors(Background.Paper, dark = true))
        // the same page either way: only the colours change
        assertEquals(paperColors(false).sketch, d.sketch)
    }

    @Test fun wallpaperModeAndSolStayAsTheyWere() {
        assertFalse(homeColors(Background.Wallpaper, dark = true).sketchbook)
        assertFalse(SolColors.sketchbook)
        assertEquals(Color(0xFFF3EBDD), SolColors.background)
        assertEquals(Color(0xFFD4A23A), SolColors.gold)
    }

    @Test fun officialPaletteAndLogoAmber() {
        assertEquals(Color(0xFFFFC21A), NumioBrand.LOGO_AMBER)
        assertEquals(0xFFFFC21A, PortalRules.GOLD)          // the logo's own colour, unchanged
        assertEquals(Color(0xFFF6F4EE), NumioBrand.PAPER)   // the website's paper, unchanged
        assertEquals(0xFFE9A800.toInt(), Tone.GOLD.argb)
        assertEquals(0xFFE9A800.toInt(), SketchIcons.GOLD)
        assertEquals(0xFFEFE6D3.toInt(), SketchIcons.PAPER)
        // drawings in each theme
        assertEquals(0xFFF1EEE6.toInt(), SketchPalette.DARK.ink)
        assertEquals(0xFFFFC21A.toInt(), SketchPalette.DARK.gold)
        assertEquals(0xFF0F0F0E.toInt(), SketchPalette.DARK.paper)
        assertEquals(0xFFF6F4EE.toInt(), SketchPalette.LIGHT_BRAND.paper)
        // each palette keeps its own icons
        assertEquals(3, setOf(SketchPalette.LIGHT_WARM.id, SketchPalette.LIGHT_BRAND.id, SketchPalette.DARK.id).size)
    }
}
