package com.rr.numio.launcher.sketch

import com.rr.numio.launcher.ui.SolColors
import com.rr.numio.launcher.ui.homeColors
import com.rr.numio.launcher.ui.paperColors
import com.rr.numio.launcher.data.Background
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SketchRolesTest {

    private val roles = mapOf(
        SketchKind.Phone to setOf("com.samsung.android.dialer", "com.samsung.android.app.contacts"),
        SketchKind.Contacts to setOf("com.samsung.android.app.contacts"),
        SketchKind.Browser to setOf("com.sec.android.app.sbrowser", "com.android.chrome"),
        SketchKind.Clock to setOf("com.sec.android.app.clockpackage"),
    )

    @Test fun anAppDoingOneJobGetsThatSymbol() {
        assertEquals(SketchKind.Phone, SketchRoles.kindFor("com.samsung.android.dialer", "Phone", roles))
        assertEquals(SketchKind.Browser, SketchRoles.kindFor("com.android.chrome", "Chrome", roles))
        assertEquals(SketchKind.Clock, SketchRoles.kindFor("com.sec.android.app.clockpackage", "Clock", roles))
    }

    @Test fun anyOtherAppFallsBackToItsOwnMarkOrLetter() {
        assertNull(SketchRoles.kindFor("com.whatsapp", "WhatsApp", roles))
        assertNull(SketchRoles.kindFor("org.mozilla.firefox", "Firefox", roles))  // installed browsers keep their look
        assertNull(SketchRoles.kindFor("com.samsung.android.dialer", "Phone", emptyMap()))
    }

    @Test fun onePackageWithTwoJobsIsSplitByTheAppsName() {
        val pkg = "com.samsung.android.app.contacts"
        assertEquals(SketchKind.Contacts, SketchRoles.kindFor(pkg, "Contacts", roles))
        assertEquals(SketchKind.Phone, SketchRoles.kindFor(pkg, "Phone", roles))
        // no hint in the name: the first job in the list
        assertEquals(SketchKind.Phone, SketchRoles.kindFor(pkg, "Galaxy", roles))
    }

    @Test fun everyJobHasAWayToBeFound() {
        val intents = SketchRoles.intents()
        for (k in SketchKind.entries) assertTrue("$k has no intent", intents[k].orEmpty().isNotEmpty())
    }

    @Test fun sketchSymbolsAreForInkOnly() {
        // AppIcon draws SketchIcons only when the theme's colours say sketch; Sol uses its own icons
        assertFalse(SolColors.sketch)
        assertTrue(paperColors(false).sketch)
        assertTrue(paperColors(true).sketch)
        assertTrue(homeColors(Background.Wallpaper, false).sketch)
    }
}
