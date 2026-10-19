package com.rr.numio.launcher.sketch

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rr.numio.launcher.data.HomePrefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SketchChoicesTest {

    @Test fun savedTextRoundTripsAndDropsUnknownChoices() {
        val map = mapOf("com.rr.numio.calendar/.Main#0" to "Calendar", "org.mozilla.fennec_fdroid/.App#0" to SketchChoices.OWN)
        assertEquals(map, SketchChoices.parse(SketchChoices.serialize(map)))
        assertEquals(emptyMap<String, String>(), SketchChoices.parse("a/.B#0\tNotAThing\nbroken line"))
        assertEquals(emptyMap<String, String>(), SketchChoices.parse(null))
    }

    @Test fun aHandPickedDrawingBeatsTheAutomaticRules() {
        assertEquals(SketchKind.Calendar, SketchChoices.decide("Calendar") { null })
        assertEquals(SketchKind.Calendar, SketchChoices.decide("Calendar") { SketchKind.Phone })
        assertNull(SketchChoices.decide(SketchChoices.OWN) { SketchKind.Browser })   // keep its own look
        assertEquals(SketchKind.Browser, SketchChoices.decide(null) { SketchKind.Browser })   // automatic
    }

    @Test fun choicesAreSavedPerAppAndCanGoBackToAutomatic() = runBlocking {
        val prefs = HomePrefs(ApplicationProvider.getApplicationContext())
        prefs.setSketchChoice("com.rr.numio.calendar/.Main#0", "Calendar")
        prefs.setSketchChoice("org.fossify.filemanager/.Main#0", "Files")
        assertEquals(
            mapOf("com.rr.numio.calendar/.Main#0" to "Calendar", "org.fossify.filemanager/.Main#0" to "Files"),
            HomePrefs(ApplicationProvider.getApplicationContext()).sketchChoices.first(),
        )
        prefs.setSketchChoice("com.rr.numio.calendar/.Main#0", null)
        assertEquals(mapOf("org.fossify.filemanager/.Main#0" to "Files"), prefs.sketchChoices.first())
    }
}
