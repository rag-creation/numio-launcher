package com.rr.numio.launcher.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchTest {

    private val apps = listOf(
        "Calculator", "Camera", "Chrome", "Clock by Numio", "Ezhuthola", "Gmail",
        "Google Maps", "Messages", "Phone", "Scam Detector", "Settings",
        "Weather Hub", "WhatsApp", "YouTube", "YouTube Music",
    )

    private fun top(q: String) = AppSearch.rank(q, apps) { it }.first()

    @Test fun emptyQueryKeepsEverythingInOrder() {
        assertEquals(apps, AppSearch.rank("  ", apps) { it })
    }

    @Test fun startOfNameWins() {
        assertEquals("WhatsApp", top("wh"))
        assertEquals("Camera", top("cam"))
        assertEquals("Gmail", top("gm"))
    }

    @Test fun exactNameBeatsLongerName() {
        assertEquals("YouTube", top("youtube"))
    }

    @Test fun wordInsideName() {
        assertEquals("Google Maps", top("maps"))
        assertEquals("YouTube Music", top("music"))
        // "App" inside WhatsApp counts as a word
        assertEquals("WhatsApp", top("app"))
    }

    @Test fun initials() {
        assertEquals("YouTube", top("yt"))
        assertEquals("YouTube Music", top("ytm"))
        assertEquals("Clock by Numio", top("cbn"))
    }

    @Test fun lettersInOrder() {
        assertEquals("Ezhuthola", top("ezhla"))
    }

    @Test fun ignoresCaseAndAccents() {
        assertEquals(1000, AppSearch.score("Café", "CAFE"))
    }

    @Test fun noMatch() {
        assertTrue(AppSearch.rank("xyzzy", apps) { it }.isEmpty())
        assertEquals(0, AppSearch.score("Phone", "q"))
    }
}
