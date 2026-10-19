package com.rr.numio.launcher.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The logo's settings really are saved, each on its own, and survive a fresh read. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class PortalPrefsTest {

    @Test fun eachSettingIsSavedAndReadBack() = runBlocking {
        val prefs = HomePrefs(ApplicationProvider.getApplicationContext())
        assertEquals(PortalSettings(), prefs.portal.first())  // defaults: shown, unset action, official gold

        val chosen = PortalSettings(visible = true, action = PortalAction.Selected, color = PortalColor.Olive, apps = listOf("wa/.Main#0", "cal/.A#0"))
        prefs.setPortal(chosen)
        assertEquals(chosen, prefs.portal.first())

        // hide it: colour, action and apps stay
        prefs.setPortal(chosen.copy(visible = false))
        assertEquals(chosen.copy(visible = false), HomePrefs(ApplicationProvider.getApplicationContext()).portal.first())

        // tap off, then back to "never chosen"
        prefs.setPortal(chosen.copy(action = PortalAction.Off))
        assertEquals(PortalAction.Off, prefs.portal.first().action)
        prefs.setPortal(chosen.copy(action = null))
        assertEquals(null, prefs.portal.first().action)
        assertEquals(listOf("wa/.Main#0", "cal/.A#0"), prefs.portal.first().apps)
    }
}
