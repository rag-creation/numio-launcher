package com.rr.numio.launcher.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import com.rr.numio.launcher.data.InkDrawing
import com.rr.numio.launcher.data.Slot
import com.rr.numio.launcher.sketch.SketchPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Both Ink drawings are decoration: a long-press on the drawing reaches the home menu
 * underneath, and apps placed over the drawing still drag and drop.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class InkArtworkTouchTest {

    @get:Rule
    val rule = createComposeRule()

    private var menu = false
    private var dropped: Pair<Slot, Slot>? = null

    private fun setUp(palette: SketchPalette, drawing: InkDrawing = InkDrawing.DEFAULT) {
        rule.setContent {
            val placement = rememberPlacement(onDrop = { from, to -> dropped = from to to })
            Box(
                Modifier
                    .size(360.dp, 400.dp)
                    .pointerInput(Unit) { detectTapGestures(onLongPress = { menu = true }) }
                    .testTag("area")
            ) {
                InkLandscape(palette, Modifier.fillMaxSize(), drawing)
                Row {
                    Box(
                        Modifier
                            .size(90.dp)
                            .appCell(placement, Slot(Slot.HOME, 0), "app", onTap = {}, onHoldStart = {}, onMenu = {})
                            .testTag("app")
                    )
                    Box(Modifier.size(90.dp).dropCell(placement, Slot(Slot.HOME, 1)))
                }
            }
        }
    }

    private fun longPressOnTheDrawing(palette: SketchPalette, drawing: InkDrawing = InkDrawing.DEFAULT) {
        setUp(palette, drawing)
        rule.onNodeWithTag("area").performTouchInput {
            // low down, where the drawing is
            down(Offset(width / 2f, height * 0.8f))
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            up()
        }
        rule.runOnIdle { assertTrue(menu) }
    }

    private fun dragOverTheDrawing(palette: SketchPalette, drawing: InkDrawing = InkDrawing.DEFAULT) {
        setUp(palette, drawing)
        rule.onNodeWithTag("app").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            repeat(10) { moveBy(Offset(width * 0.1f, 0f)) }
            up()
        }
        rule.runOnIdle { assertEquals(Slot(Slot.HOME, 0) to Slot(Slot.HOME, 1), dropped) }
    }

    @Test fun nightSceneLetsLongPressThrough() = longPressOnTheDrawing(SketchPalette.DARK)
    @Test fun sketchbookLetsLongPressThrough() = longPressOnTheDrawing(SketchPalette.LIGHT_WARM)
    @Test fun appsDragOverTheNightScene() = dragOverTheDrawing(SketchPalette.DARK)
    @Test fun appsDragOverTheSketchbook() = dragOverTheDrawing(SketchPalette.LIGHT_WARM)
    @Test fun nightLakeOnPaperLetsLongPressThrough() = longPressOnTheDrawing(SketchPalette.LIGHT_WARM, InkDrawing.NightLake)
    @Test fun appsDragOverTheNightLakeOnPaper() = dragOverTheDrawing(SketchPalette.LIGHT_WARM, InkDrawing.NightLake)
}
