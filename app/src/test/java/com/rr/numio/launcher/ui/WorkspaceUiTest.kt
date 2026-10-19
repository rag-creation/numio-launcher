package com.rr.numio.launcher.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rr.numio.launcher.R
import com.rr.numio.launcher.data.WorkState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class WorkspaceUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val colors = SolColors

    @Test fun portalUsesTheOfficialMarkAndOpensTheWorkspace() {
        assertEquals(R.drawable.numio_mark, NumioMark.RES)
        var opened = 0
        rule.setContent {
            PortalButton(tint = null, description = PORTAL_DESCRIPTION, onTap = { opened++ }, onLongPress = {})
        }
        rule.onNodeWithContentDescription(PORTAL_DESCRIPTION).assertHasClickAction().performClick()
        rule.runOnIdle { assertEquals(1, opened) }
    }

    private fun show(state: WorkState, onClose: () -> Unit = {}, onTurnOn: () -> Unit = {}) {
        rule.setContent {
            WorkspaceContent(colors, state, onClose = onClose, onTurnOn = onTurnOn) { dimmed ->
                Text(if (dimmed) "apps (dimmed)" else "apps")
            }
        }
    }

    @Test fun readyShowsTheApps() {
        show(WorkState.Ready)
        rule.onNodeWithText("apps").assertExists()
        rule.onNodeWithText("Workspace").assertExists()
    }

    @Test fun closeButtonGoesBackHome() {
        var closed = 0
        show(WorkState.Ready, onClose = { closed++ })
        rule.onNodeWithContentDescription("Back to home").performClick()
        rule.runOnIdle { assertEquals(1, closed) }
    }

    @Test fun systemBackGoesBackHome() {
        var closed = 0
        show(WorkState.Ready, onClose = { closed++ })
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.runOnIdle { assertEquals(1, closed) }
    }

    @Test fun pausedExplainsAndOnlyAsksAndroidWhenTapped() {
        var asked = 0
        show(WorkState.Paused, onTurnOn = { asked++ })
        rule.onNodeWithText("Work apps are paused").assertExists()
        rule.onNodeWithText("apps (dimmed)").assertExists()
        rule.runOnIdle { assertEquals(0, asked) }  // nothing happens by itself
        rule.onNodeWithText("Turn on work apps").performClick()
        rule.runOnIdle { assertEquals(1, asked) }
    }

    @Test fun lockedExplainsAndStillListsApps() {
        show(WorkState.Locked)
        rule.onNodeWithText("Your work profile is locked").assertExists()
        rule.onNodeWithText("apps").assertExists()
        rule.onNodeWithText("Turn on work apps").assertDoesNotExist()
    }

    @Test fun emptyAndUnavailableShowANoteInsteadOfApps() {
        show(WorkState.Empty)
        rule.onNodeWithText("No work apps yet").assertExists()
        rule.onNodeWithText("apps").assertDoesNotExist()
    }

    @Test fun unavailableShowsANote() {
        show(WorkState.Unavailable)
        rule.onNodeWithText("Work apps can't be shown right now").assertExists()
        rule.onNodeWithText("apps").assertDoesNotExist()
    }
}
