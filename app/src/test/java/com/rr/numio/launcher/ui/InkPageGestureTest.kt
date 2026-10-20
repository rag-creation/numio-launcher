package com.rr.numio.launcher.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rr.numio.launcher.data.Slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Ink's home, built the way CanvasScreen nests it: the whole page listens for a swipe up
 * (opens the Shelf); on it, page cells where empty ones open the home menu on long-press
 * and apps use appCell(); below, a dock app. Real touches check that the gestures don't
 * fight: holding an app drags it (never the home menu, never the Shelf), and a quick swipe
 * starting on an app still opens the Shelf.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class InkPageGestureTest {

    @get:Rule
    val rule = createComposeRule()

    private var dropped: Pair<Slot, Slot>? = null
    private var appMenu = false
    private var homeMenu = false
    private var shelf = false
    private var tapped = false

    private fun page(i: Int) = Slot(Slot.HOME, i)
    private val dock0 = Slot(Slot.DOCK, 0)

    private fun setUp() {
        rule.setContent {
            val placement = rememberPlacement(onDrop = { from, to -> dropped = from to to })
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var dragged = 0f
                        detectVerticalDragGestures(
                            onDragStart = { dragged = 0f },
                            onDragEnd = { if (dragged < -100f) shelf = true },
                            onVerticalDrag = { _, dy -> dragged += dy },
                        )
                    }
            ) {
                Column {
                    Row {
                        Box(
                            Modifier
                                .size(90.dp)
                                .appCell(
                                    placement, page(0), "app",
                                    onTap = { tapped = true },
                                    onHoldStart = {},
                                    onMenu = { appMenu = true },
                                )
                                .testTag("app")
                        )
                        Box(
                            Modifier
                                .size(90.dp)
                                .dropCell(placement, page(1))
                                .pointerInput(Unit) { detectTapGestures(onLongPress = { homeMenu = true }) }
                                .testTag("empty")
                        )
                    }
                    Spacer(Modifier.height(200.dp))
                    Box(
                        Modifier
                            .size(90.dp)
                            .appCell(placement, dock0, "dockApp", onTap = {}, onHoldStart = {}, onMenu = {})
                            .testTag("dock")
                    )
                }
            }
        }
    }

    @Test
    fun holdAnAppAndMoveIt_placesItOnTheEmptyCell() {
        setUp()
        rule.onNodeWithTag("app").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            repeat(10) { moveBy(Offset(width * 0.1f, 0f)) }
            up()
        }
        rule.runOnIdle {
            assertEquals(page(0) to page(1), dropped)
            assertFalse(homeMenu)
            assertFalse(appMenu)
            assertFalse(shelf)
        }
    }

    @Test
    fun dragADockAppUpOntoThePage_neverOpensTheShelf() {
        setUp()
        rule.onNodeWithTag("dock").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            // up to the page's empty cell, far more than a Shelf swipe
            val target = Offset(width * 1f, -(height + 200.dp.toPx()))
            repeat(20) { moveBy(target / 20f) }
            up()
        }
        rule.runOnIdle {
            assertEquals(dock0 to page(1), dropped)
            assertFalse(shelf)
        }
    }

    @Test
    fun holdAnAppWithoutMoving_opensOnlyTheAppMenu() {
        setUp()
        rule.onNodeWithTag("app").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            up()
        }
        rule.runOnIdle {
            assertTrue(appMenu)
            assertFalse(homeMenu)
            assertNull(dropped)
        }
    }

    @Test
    fun longPressAnEmptySpot_opensTheHomeMenu() {
        setUp()
        rule.onNodeWithTag("empty").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            up()
        }
        rule.runOnIdle {
            assertTrue(homeMenu)
            assertFalse(appMenu)
        }
    }

    @Test
    fun quickSwipeUpFromAnApp_stillOpensTheShelf() {
        setUp()
        rule.onNodeWithTag("app").performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(0f, -30f)) }
            up()
        }
        rule.runOnIdle {
            assertTrue(shelf)
            assertNull(dropped)
            assertFalse(tapped)
        }
    }

    @Test
    fun tapAnApp_opensIt() {
        setUp()
        rule.onNodeWithTag("app").performTouchInput {
            down(center)
            advanceEventTime(50)
            up()
        }
        rule.runOnIdle {
            assertTrue(tapped)
            assertNull(dropped)
        }
    }
}
