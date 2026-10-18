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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Real touch input against the same reorderSlot() the home rows and dock use:
 * three 80dp slots in a row, like icons.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ReorderGestureTest {

    @get:Rule
    val rule = createComposeRule()

    private var moved: Pair<Int, Int>? = null
    private var tapped = -1
    private var menu = -1

    private fun setUp() {
        rule.setContent {
            val reorder = rememberReorder { from, to -> moved = from to to }
            Row {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .size(80.dp)
                            .reorderSlot(
                                reorder, i, "app$i",
                                onTap = { tapped = i },
                                onHoldStart = {},
                                onMenu = { menu = i },
                            )
                            .testTag("slot$i")
                    )
                }
            }
        }
    }

    @Test
    fun holdAndDragToTheThirdSlot_movesIt() {
        setUp()
        rule.onNodeWithTag("slot0").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            // move in small steps, like a finger, two slots to the right
            repeat(10) { moveBy(Offset(width * 0.2f, 0f)) }
            up()
        }
        rule.runOnIdle {
            assertEquals(0 to 2, moved)
            assertEquals(-1, menu)
            assertEquals(-1, tapped)
        }
    }

    @Test
    fun holdAndDragBackwards_movesIt() {
        setUp()
        rule.onNodeWithTag("slot2").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            repeat(10) { moveBy(Offset(-width * 0.1f, 0f)) }
            up()
        }
        rule.runOnIdle { assertEquals(2 to 1, moved) }
    }

    @Test
    fun holdAndReleaseWithoutMoving_opensTheMenuOnly() {
        setUp()
        rule.onNodeWithTag("slot1").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            up()
        }
        rule.runOnIdle {
            assertEquals(1, menu)
            assertNull(moved)
        }
    }

    @Test
    fun quickTap_opensTheApp() {
        setUp()
        rule.onNodeWithTag("slot1").performTouchInput {
            down(center)
            advanceEventTime(50)
            up()
        }
        rule.runOnIdle {
            assertEquals(1, tapped)
            assertEquals(-1, menu)
            assertNull(moved)
        }
    }
}
