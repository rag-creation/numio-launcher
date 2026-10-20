package com.rr.numio.launcher.ui

import androidx.compose.ui.graphics.Color
import com.rr.numio.launcher.data.Background
import com.rr.numio.launcher.data.PortalRules
import com.rr.numio.launcher.sketch.SketchIcons
import com.rr.numio.launcher.sketch.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ink's sketchbook page uses Numio's official colours; Sol and the logo are untouched. */
class SketchbookThemeTest {

    @Test fun inkPaperIsTheSketchbookWithOfficialInkAndAmber() {
        val ink = paperColors(dark = false)
        assertEquals(Color(0xFFEFE6D3), ink.background)
        assertEquals(Color(0xFF191815), ink.ink)
        assertEquals(Color(0xFFE9A800), ink.gold)          // accents on paper
        assertEquals(Color(0xFF7A5600), ink.goldText)      // amber text on paper
        assertTrue(ink.sketchbook && ink.sketch)
        // a page of paper is always light, whatever the system's dark mode
        assertEquals(ink, paperColors(dark = true))
        assertEquals(ink, homeColors(Background.Paper, dark = true))
    }

    @Test fun wallpaperModeAndSolStayAsTheyWere() {
        assertFalse(homeColors(Background.Wallpaper, dark = false).sketchbook)
        assertFalse(SolColors.sketchbook)
        assertEquals(Color(0xFFF3EBDD), SolColors.background)
        assertEquals(Color(0xFFD4A23A), SolColors.gold)
        assertEquals(SolColors.gold, SolColors.goldText)
    }

    @Test fun officialPaletteAndLogoAmber() {
        assertEquals(Color(0xFFFFC21A), NumioBrand.LOGO_AMBER)
        assertEquals(0xFFFFC21A, PortalRules.GOLD)          // the logo's own colour, unchanged
        assertEquals(Color(0xFFF6F4EE), NumioBrand.PAPER)   // the website's paper, unchanged
        // icons' amber details and badges use the accent amber
        assertEquals(0xFFE9A800.toInt(), Tone.GOLD.argb)
        assertEquals(0xFFE9A800.toInt(), SketchIcons.GOLD)
        // icon circles are the page itself: ink circles drawn on the paper
        assertEquals(0xFFEFE6D3.toInt(), SketchIcons.PAPER)
    }
}
