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
            SketchRoles.choose(SketchKind.Phone, listOf("org.fossify.phone"), listOf(h("org.fossify.phone", false), h("com.samsung.android.dialer", true)), null),
        )
        // Fennec as the default browser
        assertEquals(setOf("org.mozilla.fennec_fdroid"), SketchRoles.choose(SketchKind.Browser, listOf("org.mozilla.fennec_fdroid", null), emptyList(), null))
    }

    @Test fun withoutADefaultThePreinstalledAppsCount() {
        assertEquals(
            setOf("com.sec.android.app.camera"),
            SketchRoles.choose(SketchKind.Camera, listOf("android"), listOf(h("com.sec.android.app.camera", true), h("net.sourceforge.opencamera", false)), null),
        )
    }

    @Test fun withNeitherOnlyASingleAppCounts() {
        // one calendar app installed: it gets the symbol
        assertEquals(setOf("org.fossify.calendar"), SketchRoles.choose(SketchKind.Calendar, listOf(null), listOf(h("org.fossify.calendar", false)), null))
        // three calendars and no default: none is singled out, each keeps its own look
        assertEquals(
            emptySet<String>(),
            SketchRoles.choose(SketchKind.Calendar, listOf("android"), listOf(h("org.fossify.calendar", false), h("com.goodwy.calendar", false), h("com.rr.numio.calendar", false)), mapOf("org.fossify.calendar" to "Calendar", "com.goodwy.calendar" to "Calendar", "com.rr.numio.calendar" to "Calendar")),
        )
    }

    @Test fun contactsAppsAreFoundByCreateContactToo() {
        // Fossify Contacts declares no APP_CONTACTS, only "insert a contact"
        val contacts = SketchRoles.intents()[SketchKind.Contacts].orEmpty()
        assertTrue(contacts.any { it.action == android.content.Intent.ACTION_INSERT && it.type == "vnd.android.cursor.dir/contact" })
        // the only app that creates contacts gets the person symbol
        assertEquals(setOf("org.fossify.contacts"), SketchRoles.choose(SketchKind.Contacts, listOf(null, "org.fossify.contacts"), listOf(h("org.fossify.contacts", false)), null))
        assertEquals(SketchKind.Contacts, SketchRoles.kindFor("org.fossify.contacts", "Contacts", mapOf(SketchKind.Contacts to setOf("org.fossify.contacts"))))
    }

    @Test fun hiddenAppsNeverTakeTheSymbolFromTheOneYouUse() {
        // Samsung Contacts still installed but hidden from the app list; Fossify Contacts shown
        val shown = mapOf("org.fossify.contacts" to "Contacts", "im.molly.app" to "Molly")
        assertEquals(
            setOf("org.fossify.contacts"),
            SketchRoles.choose(
                SketchKind.Contacts,
                listOf("com.samsung.android.app.contacts", "android"),
                listOf(h("com.samsung.android.app.contacts", true), h("org.fossify.contacts", false)),
                shown,
            ),
        )
    }

    @Test fun ofSeveralInstalledAppsTheOneNamedForTheJobWins() {
        // a messenger that can also "add a contact" doesn't take the person symbol
        val shown = mapOf("org.fossify.contacts" to "Contacts", "im.molly.app" to "Molly")
        assertEquals(
            setOf("org.fossify.contacts"),
            SketchRoles.choose(SketchKind.Contacts, listOf("android"), listOf(h("org.fossify.contacts", false), h("im.molly.app", false)), shown),
        )
    }

    @Test fun aVisibleBuiltInAppThatCanAlsoAddContactsDoesntWin() {
        val shown = mapOf("org.fossify.contacts" to "Contacts", "com.samsung.android.dialer" to "Phone")
        assertEquals(
            setOf("org.fossify.contacts"),
            SketchRoles.choose(SketchKind.Contacts, listOf("android"), listOf(h("com.samsung.android.dialer", true), h("org.fossify.contacts", false)), shown),
        )
        // a lone calendar app that isn't named "Calendar" (Etar) still gets it
        assertEquals(setOf("ws.xsoh.etar"), SketchRoles.choose(SketchKind.Calendar, listOf(null), listOf(h("ws.xsoh.etar", false)), mapOf("ws.xsoh.etar" to "Etar")))
    }

    @Test fun anAppNamedExactlyForTheJobBeatsOneThatMentionsIt() {
        val shown = mapOf("org.fossify.gallery" to "Gallery", "com.google.ai.edge.gallery" to "Edge Gallery", "app.kaii.photos" to "Photos")
        assertEquals(
            setOf("org.fossify.gallery"),
            SketchRoles.choose(
                SketchKind.Gallery, listOf("android"),
                listOf(h("org.fossify.gallery", false), h("com.google.ai.edge.gallery", false), h("app.kaii.photos", false)),
                shown,
            ),
        )
        // several mail apps and no default: none is singled out, each keeps its own look
        val mail = mapOf("com.fsck.k9" to "K-9 Mail", "ch.protonmail.android" to "Proton Mail", "de.tutao.tutanota" to "Tuta")
        assertEquals(
            emptySet<String>(),
            SketchRoles.choose(SketchKind.Email, listOf("android"), mail.keys.map { h(it, false) }, mail),
        )
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
