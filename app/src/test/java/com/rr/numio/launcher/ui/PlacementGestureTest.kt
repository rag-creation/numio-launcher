package com.rr.numio.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rr.numio.launcher.data.Slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Real touch input against the same appCell() / dropCell() the home and dock use:
 * two app cells and one empty cell in a row, 80dp each.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class PlacementGestureTest {

    @get:Rule
    val rule = createComposeRule()

    private var dropped: Pair<Slot, Slot>? = null
    private var tapped = -1
    private var menu = -1

    private fun home(i: Int) = Slot(Slot.HOME, i)

    private fun setUp() {
        rule.setContent {
            val placement = rememberPlacement(onDrop = { from, to -> dropped = from to to })
            Row {
                repeat(2) { i ->
                    Box(
                        Modifier
                            .size(80.dp)
                            .appCell(
                                placement, home(i), "app$i",
                                onTap = { tapped = i },
                                onHoldStart = {},
                                onMenu = { menu = i },
                            )
                            .testTag("app$i")
                    )
                }
                Box(Modifier.size(80.dp).dropCell(placement, home(2)).testTag("empty2"))
            }
        }
    }

    @Test
    fun dragIntoTheEmptyCell_dropsThere() {
        setUp()
        rule.onNodeWithTag("app0").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            repeat(10) { moveBy(Offset(width * 0.2f, 0f)) }
            up()
        }
        rule.runOnIdle {
            assertEquals(home(0) to home(2), dropped)
            assertEquals(-1, menu)
            assertEquals(-1, tapped)
        }
    }

    @Test
    fun dragOntoTheOtherApp_dropsThere() {
        setUp()
        rule.onNodeWithTag("app1").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            repeat(10) { moveBy(Offset(-width * 0.1f, 0f)) }
            up()
        }
        rule.runOnIdle { assertEquals(home(1) to home(0), dropped) }
    }

    @Test
    fun dragFarFromEveryCell_changesNothing() {
        setUp()
        rule.onNodeWithTag("app0").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            repeat(10) { moveBy(Offset(0f, height * 0.5f)) }
            up()
        }
        rule.runOnIdle { assertNull(dropped) }
    }

    @Test
    fun holdAndReleaseWithoutMoving_opensTheMenuOnly() {
        setUp()
        rule.onNodeWithTag("app1").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            up()
        }
        rule.runOnIdle {
            assertEquals(1, menu)
            assertNull(dropped)
        }
    }

    @Test
    fun quickTap_opensTheApp() {
        setUp()
        rule.onNodeWithTag("app1").performTouchInput {
            down(center)
            advanceEventTime(50)
            up()
        }
        rule.runOnIdle {
            assertEquals(1, tapped)
            assertEquals(-1, menu)
            assertNull(dropped)
        }
    }
}
