package com.rr.numio.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SunSpotTest {

    @Test fun onTheA73TheSunIsExactlyWhereItWasDrawn() {
        // 1080×2400 screen = the artwork's own size, nothing cropped
        assertEquals(837f to 972f, SunSpot.onScreen(SolWallpaper.Petal, 1080f, 2400f))
        assertEquals(1000f to 880f, SunSpot.onScreen(SolWallpaper.Garden, 1080f, 2400f))
    }

    @Test fun shorterScreensCropMostlyFromTheSky() {
        // 1080×2160: 240px trimmed, 1.4 × half of it (168px) from the top
        val (x, y) = SunSpot.onScreen(SolWallpaper.Petal, 1080f, 2160f)
        assertEquals(837f, x, 0.01f)
        assertEquals(972f - 168f, y, 0.01f)
    }

    @Test fun scalesWithTheScreen() {
        // half-size box: everything halves
        val (x, y) = SunSpot.onScreen(SolWallpaper.Petal, 540f, 1200f)
        assertEquals(418.5f, x, 0.01f)
        assertEquals(486f, y, 0.01f)
    }
}
