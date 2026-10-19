package com.rr.numio.launcher.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rr.numio.launcher.data.HomeTheme
import com.rr.numio.launcher.data.PortalAction
import com.rr.numio.launcher.data.PortalColor
import com.rr.numio.launcher.data.PortalSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class PortalUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    // ── tap vs long-press ───────────────────────────────────────────────────

    private var taps = 0
    private var menus = 0

    private fun logo() = rule.setContent {
        PortalButton(tint = null, description = "Numio logo", onTap = { taps++ }, onLongPress = { menus++ })
    }

    @Test fun tapDoesTheActionOnly() {
        logo()
        rule.onNodeWithContentDescription("Numio logo").performClick()
        rule.runOnIdle { assertEquals(1, taps); assertEquals(0, menus) }
    }

    @Test fun longPressOpensTheMenuAndNeverAlsoTaps() {
        logo()
        rule.onNodeWithContentDescription("Numio logo").performTouchInput { longClick() }
        rule.runOnIdle { assertEquals(0, taps); assertEquals(1, menus) }
    }

    // ── the customization menu ──────────────────────────────────────────────

    private var applied: PortalSettings? = null
    private var cancelled = 0

    private fun menu(initial: PortalSettings, hasWork: Boolean, theme: HomeTheme = HomeTheme.Sol) = rule.setContent {
        PortalSettingsContent(
            initial = initial, hasWork = hasWork, theme = theme, chosenCount = initial.apps.size,
            onChooseApps = {}, onCancel = { cancelled++ }, onApply = { applied = it },
        )
    }

    @Test fun colourIsPreviewedFirstAndOnlySavedOnApply() {
        menu(PortalSettings(color = PortalColor.Gold), hasWork = true)
        rule.onNodeWithContentDescription("Preview, Numio gold").performScrollTo().assertExists()
        rule.onNodeWithContentDescription("Olive").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Preview, Olive").performScrollTo().assertExists()
        rule.runOnIdle { assertNull(applied) }
        rule.onNodeWithText("Apply").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(PortalColor.Olive, applied!!.color) }
    }

    @Test fun cancelSavesNothing() {
        menu(PortalSettings(), hasWork = true)
        rule.onNodeWithContentDescription("Charcoal").performScrollTo().performClick()
        rule.onNodeWithText("Cancel").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, cancelled); assertNull(applied) }
    }

    @Test fun withoutAWorkProfile_workspaceIsMarkedUnavailableAndCantBePicked() {
        menu(PortalSettings(action = null, apps = listOf("a#0")), hasWork = false)
        rule.onNodeWithText("Not available: no work profile on this phone").performScrollTo().assertExists()
        rule.onNodeWithText("Workspace").performScrollTo().performClick()
        rule.onNodeWithText("Apply").performScrollTo().performClick()
        // still unset, so it keeps meaning "Selected apps" here
        rule.runOnIdle { assertNull(applied!!.action); assertEquals(listOf("a#0"), applied!!.apps) }
    }

    @Test fun pickingEachTapMode() {
        menu(PortalSettings(), hasWork = true)
        rule.onNodeWithText("Nothing").performScrollTo().performClick()
        rule.onNodeWithText("Apply").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(PortalAction.Off, applied!!.action) }
    }

    @Test fun selectedAppsMode() {
        menu(PortalSettings(action = PortalAction.Workspace), hasWork = true)
        rule.onNodeWithText("Selected apps").performScrollTo().performClick()
        rule.onNodeWithText("Apply").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(PortalAction.Selected, applied!!.action) }
    }

    @Test fun hidingKeepsColourActionAndApps() {
        val start = PortalSettings(visible = true, action = PortalAction.Selected, color = PortalColor.Olive, apps = listOf("a#0", "b#0"))
        menu(start, hasWork = true)
        rule.onNodeWithText("Show on home screen").performScrollTo().performClick()
        rule.onNodeWithText("Apply").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(start.copy(visible = false), applied) }
    }
}
