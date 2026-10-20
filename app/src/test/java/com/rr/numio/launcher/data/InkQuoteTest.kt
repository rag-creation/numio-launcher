package com.rr.numio.launcher.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class InkQuoteTest {

    @Test fun shownByDefaultThenEditedWordsAndHidingAreSaved() = runBlocking {
        val prefs = HomePrefs(ApplicationProvider.getApplicationContext())
        assertEquals(InkQuote("Good things take time.", visible = true), prefs.inkQuote.first())
        prefs.setInkQuote(InkQuote("  Slow   is smooth.  ", visible = false))
        assertEquals(InkQuote("Slow is smooth.", visible = false), HomePrefs(ApplicationProvider.getApplicationContext()).inkQuote.first())
        prefs.setInkQuote(InkQuote("x".repeat(200), visible = true))
        assertEquals(InkQuote.MAX_LENGTH, prefs.inkQuote.first().text.length)
    }

    @Test fun appearanceIsLightByDefaultAndIsSaved() = runBlocking {
        val prefs = HomePrefs(ApplicationProvider.getApplicationContext())
        assertEquals(InkAppearance.Light, prefs.inkAppearance.first())
        assertTrue(prefs.inkWarmPaper.first())
        prefs.setInkAppearance(InkAppearance.Dark)
        prefs.setInkWarmPaper(false)
        val again = HomePrefs(ApplicationProvider.getApplicationContext())
        assertEquals(InkAppearance.Dark, again.inkAppearance.first())
        assertFalse(again.inkWarmPaper.first())
        prefs.setInkAppearance(InkAppearance.System)
        assertEquals(InkAppearance.System, HomePrefs(ApplicationProvider.getApplicationContext()).inkAppearance.first())
        prefs.setInkAppearance(InkAppearance.Light)
        prefs.setInkWarmPaper(true)
        assertEquals(InkAppearance.Dark, InkAppearance.of("dark"))
        assertEquals(InkAppearance.Light, InkAppearance.of("something else"))
        assertEquals(InkAppearance.Light, InkAppearance.of(null))
    }

    @Test fun lightInkStaysLightWhenThePhoneIsInDarkMode() {
        assertFalse(InkAppearance.Light.isDark(systemDark = true))
        assertFalse(InkAppearance.Light.isDark(systemDark = false))
        assertTrue(InkAppearance.Dark.isDark(systemDark = false))
        assertTrue(InkAppearance.System.isDark(systemDark = true))
        assertFalse(InkAppearance.System.isDark(systemDark = false))
        assertFalse(InkAppearance.DEFAULT.isDark(systemDark = true))
    }

    @Test fun handLettersOnlyWhenTheyCanWriteIt() {
        assertTrue(InkQuote.handwritable("Good things take time."))
        assertTrue(InkQuote.handwritable("Day 1, keep going"))
        assertFalse(InkQuote.handwritable("Don't panic!"))         // ' and ! aren't drawn
        assertFalse(InkQuote.handwritable("നല്ലത് സമയമെടുക്കും"))      // other scripts: the phone's font
        assertFalse(InkQuote.handwritable("   "))
    }

    @Test fun wrapsOnWholeWords() {
        val measure = { s: String -> s.length * 10f }
        assertEquals(listOf("GOOD THINGS", "TAKE TIME."), InkQuote.wrap("GOOD THINGS TAKE TIME.", 120f, measure))
        assertEquals(listOf("A", "VERYLONGWORD", "B"), InkQuote.wrap("A VERYLONGWORD B", 60f, measure))
        assertEquals(emptyList<String>(), InkQuote.wrap("   ", 100f, measure))
    }
}
