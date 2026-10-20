package com.rr.numio.launcher.ui

import com.rr.numio.launcher.data.Background
import com.rr.numio.launcher.data.InkAppearance
import com.rr.numio.launcher.sketch.InkArtwork
import com.rr.numio.launcher.sketch.SketchPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Which drawing the Ink page shows, worked out the same way the app does it:
 * Ink appearance setting → page colours → palette → drawing.
 */
class InkArtworkTest {

    /** What the home page draws for this setting while the phone is (or isn't) in dark mode. */
    private fun artFor(appearance: InkAppearance, phoneDark: Boolean, warm: Boolean = true) =
        InkArtwork.of(homeColors(Background.Paper, appearance.isDark(phoneDark), warm).icons)

    @Test fun lightPalettesKeepTheSketchbookLandscape() {
        assertEquals(InkArtwork.Sketchbook, InkArtwork.of(SketchPalette.LIGHT_WARM))
        assertEquals(InkArtwork.Sketchbook, InkArtwork.of(SketchPalette.LIGHT_BRAND))
    }

    @Test fun darkPaletteShowsTheNightScene() {
        assertEquals(InkArtwork.Night, InkArtwork.of(SketchPalette.DARK))
    }

    @Test fun lightInkKeepsItsDrawingEvenInSamsungDarkMode() {
        assertEquals(InkArtwork.Sketchbook, artFor(InkAppearance.Light, phoneDark = true))
        assertEquals(InkArtwork.Sketchbook, artFor(InkAppearance.Light, phoneDark = false))
        // on either paper
        assertEquals(InkArtwork.Sketchbook, artFor(InkAppearance.Light, phoneDark = true, warm = false))
    }

    @Test fun darkInkShowsTheNightSceneEvenInSamsungLightMode() {
        assertEquals(InkArtwork.Night, artFor(InkAppearance.Dark, phoneDark = false))
        assertEquals(InkArtwork.Night, artFor(InkAppearance.Dark, phoneDark = true))
    }

    @Test fun followSystemMatchesThePhone() {
        assertEquals(InkArtwork.Night, artFor(InkAppearance.System, phoneDark = true))
        assertEquals(InkArtwork.Sketchbook, artFor(InkAppearance.System, phoneDark = false))
    }

    @Test fun lightInkIsStillTheDefault() {
        assertEquals(InkArtwork.Sketchbook, artFor(InkAppearance.DEFAULT, phoneDark = true))
    }

    @Test fun solNeverUsesInkDrawings() {
        assertFalse(SolColors.sketchbook)
    }
}
