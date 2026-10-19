package com.rr.numio.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PortalRulesTest {

    @Test fun eachTapMode() {
        val s = PortalSettings()
        assertEquals(PortalTap.OpenWorkspace, PortalRules.tap(s.copy(action = PortalAction.Workspace), hasWork = true))
        assertEquals(PortalTap.OpenSelected, PortalRules.tap(s.copy(action = PortalAction.Selected), hasWork = true))
        assertEquals(PortalTap.Nothing, PortalRules.tap(s.copy(action = PortalAction.Off), hasWork = true))
    }

    @Test fun neverChosen_followsWhetherThereIsAWorkProfile() {
        assertEquals(PortalTap.OpenWorkspace, PortalRules.tap(PortalSettings(), hasWork = true))
        assertEquals(PortalTap.OpenSelected, PortalRules.tap(PortalSettings(), hasWork = false))
    }

    @Test fun workspaceChosenButNoWorkProfile_saysSoAndKeepsTheChoice() {
        val s = PortalSettings(action = PortalAction.Workspace, apps = listOf("a#0"), color = PortalColor.Olive)
        assertEquals(PortalTap.NoWorkProfile, PortalRules.tap(s, hasWork = false))
        // when the profile comes back, the same saved settings open the Workspace again
        assertEquals(PortalTap.OpenWorkspace, PortalRules.tap(s, hasWork = true))
        // and nothing about it changed meanwhile
        assertEquals(listOf("a#0"), s.apps)
        assertEquals(PortalColor.Olive, s.color)
    }

    @Test fun offIsNotHidden() {
        val off = PortalSettings(visible = true, action = PortalAction.Off)
        assertTrue(off.visible)
        assertEquals(PortalTap.Nothing, PortalRules.tap(off, hasWork = true))
        val hidden = PortalSettings(visible = false, action = PortalAction.Selected)
        assertEquals(PortalTap.OpenSelected, PortalRules.tap(hidden, hasWork = false))
    }

    @Test fun goldIsTheUntouchedArtworkOnEveryTheme() {
        assertNull(PortalRules.color(PortalColor.Gold, HomeTheme.Ink))
        assertNull(PortalRules.color(PortalColor.Gold, HomeTheme.Sol))
    }

    @Test fun autoFollowsTheTheme_fixedColoursDoNot() {
        assertNull(PortalRules.color(PortalColor.Auto, HomeTheme.Ink))
        assertEquals(PortalRules.SOL_GOLD, PortalRules.color(PortalColor.Auto, HomeTheme.Sol))
        for (c in listOf(PortalColor.Olive, PortalColor.Charcoal, PortalColor.Cream)) {
            assertEquals(PortalRules.color(c, HomeTheme.Ink), PortalRules.color(c, HomeTheme.Sol))
        }
        assertEquals(PortalRules.OLIVE, PortalRules.color(PortalColor.Olive, HomeTheme.Ink))
    }

    @Test fun savedColourIdsRoundTrip() {
        PortalColor.entries.forEach { assertEquals(it, PortalColor.of(it.id)) }
        assertEquals(PortalColor.Gold, PortalColor.of(null))
        PortalAction.entries.forEach { assertEquals(it, PortalAction.of(it.id)) }
        assertNull(PortalAction.of("nonsense"))
    }

    @Test fun lowContrastWarnings() {
        // olive sinks into the sage buttons, cream into the light ones
        assertTrue(PortalRules.lowContrast(PortalColor.Olive, PortalStyle.SageCeramic))
        assertTrue(PortalRules.lowContrast(PortalColor.Olive, PortalStyle.Extruded))
        assertTrue(PortalRules.lowContrast(PortalColor.Cream, PortalStyle.FloatingCeramic))
        assertTrue(PortalRules.lowContrast(PortalColor.Cream, PortalStyle.Glass))
        for (st in PortalStyle.entries - PortalStyle.Mustard) assertFalse(PortalRules.lowContrast(PortalColor.Gold, st))
        assertTrue(PortalRules.lowContrast(PortalColor.Gold, PortalStyle.Mustard))
        assertFalse(PortalRules.lowContrast(PortalColor.Charcoal, PortalStyle.Mustard))
        assertFalse(PortalRules.lowContrast(PortalColor.Charcoal, PortalStyle.SageCeramic))
    }

    @Test fun styleIdsRoundTripAndUnknownFallsBackToSage() {
        for (st in PortalStyle.entries) assertEquals(st, PortalStyle.of(st.id))
        assertEquals(PortalStyle.SageCeramic, PortalStyle.of(null))
        assertEquals(PortalStyle.SageCeramic, PortalStyle.of("something-old"))
        assertEquals(PortalStyle.entries.size, PortalStyle.entries.map { it.id }.toSet().size)
    }

    private data class App(val key: String, val label: String)

    @Test fun chosenAppsKeepOrderAndCountMissingOnes() {
        val personal = listOf(App("cal/.A#0", "Calendar"), App("wa/.Main#0", "WhatsApp"), App("cam/.C#0", "Camera"))
        val (found, missing) = PortalRules.chosen(listOf("wa/.Main#0", "gone/.X#0", "cal/.A#0"), personal) { it.key }
        assertEquals(listOf("WhatsApp", "Calendar"), found.map { it.label })
        assertEquals(1, missing)
    }

    @Test fun chosenAppsAreMatchedByKey_notName() {
        // a work Calendar has the same name, but a different key; only the chosen one counts
        val personal = listOf(App("cal/.A#0", "Calendar"))
        val (found, missing) = PortalRules.chosen(listOf("cal/.A#10"), personal) { it.key }
        assertTrue(found.isEmpty())
        assertEquals(1, missing)
    }

    @Test fun toggle() {
        assertEquals(listOf("a", "b"), PortalRules.toggle(listOf("a"), "b"))
        assertEquals(listOf("a"), PortalRules.toggle(listOf("a", "b"), "b"))
    }

    @Test fun mustardDiscSwitchesAGoldMarkToCharcoalOnly() {
        val base = PortalSettings(action = PortalAction.Selected, apps = listOf("a/.A#0"))
        assertEquals(base.copy(style = PortalStyle.Mustard, color = PortalColor.Charcoal), PortalRules.withStyle(base.copy(color = PortalColor.Gold), PortalStyle.Mustard))
        assertEquals(PortalColor.Charcoal, PortalRules.withStyle(base.copy(color = PortalColor.Auto), PortalStyle.Mustard).color)
        assertEquals(PortalColor.Cream, PortalRules.withStyle(base.copy(color = PortalColor.Cream), PortalStyle.Mustard).color)
        // other styles never touch the colour
        for (st in PortalStyle.entries - PortalStyle.Mustard) {
            assertEquals(base.copy(style = st), PortalRules.withStyle(base, st))
        }
    }
}
