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
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
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

    private fun h(p: String, pre: Boolean) = SketchRoles.Handler(p, pre)

    @Test fun yourDefaultAppGetsTheSymbolEvenIfYouInstalledIt() {
        // Fossify Phone set as the dialer: it wins over the preinstalled one
        assertEquals(
            setOf("org.fossify.phone"),
            SketchRoles.choose(listOf("org.fossify.phone"), listOf(h("org.fossify.phone", false), h("com.samsung.android.dialer", true))),
        )
        // Fennec as the default browser
        assertEquals(setOf("org.mozilla.fennec_fdroid"), SketchRoles.choose(listOf("org.mozilla.fennec_fdroid", null), emptyList()))
    }

    @Test fun withoutADefaultThePreinstalledAppsCount() {
        assertEquals(
            setOf("com.sec.android.app.camera"),
            SketchRoles.choose(listOf("android"), listOf(h("com.sec.android.app.camera", true), h("net.sourceforge.opencamera", false))),
        )
    }

    @Test fun withNeitherOnlyASingleAppCounts() {
        // one calendar app installed: it gets the symbol
        assertEquals(setOf("org.fossify.calendar"), SketchRoles.choose(listOf(null), listOf(h("org.fossify.calendar", false))))
        // three calendars and no default: none is singled out, each keeps its own look
        assertEquals(
            emptySet<String>(),
            SketchRoles.choose(listOf("android"), listOf(h("org.fossify.calendar", false), h("com.goodwy.calendar", false), h("com.rr.numio.calendar", false))),
        )
    }

    @Test fun contactsAppsAreFoundByCreateContactToo() {
        // Fossify Contacts declares no APP_CONTACTS, only "insert a contact"
        val contacts = SketchRoles.intents()[SketchKind.Contacts].orEmpty()
        assertTrue(contacts.any { it.action == android.content.Intent.ACTION_INSERT && it.type == "vnd.android.cursor.dir/contact" })
        // the only app that creates contacts gets the person symbol
        assertEquals(setOf("org.fossify.contacts"), SketchRoles.choose(listOf(null, "org.fossify.contacts"), listOf(h("org.fossify.contacts", false))))
        assertEquals(SketchKind.Contacts, SketchRoles.kindFor("org.fossify.contacts", "Contacts", mapOf(SketchKind.Contacts to setOf("org.fossify.contacts"))))
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
