package com.rr.numio.launcher.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rr.numio.launcher.sketch.SketchChoices
import com.rr.numio.launcher.sketch.SketchKind
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SketchIconPickerTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val picks = mutableListOf<String?>()
    private var cancelled = 0

    private fun picker(current: String?, auto: SketchKind?) = rule.setContent {
        SketchIconPickerContent("Calendar", current, auto, onPick = { picks += it }, onCancel = { cancelled++ }) { Box {} }
    }

    @Test fun pickingADrawingSendsItsName() {
        picker(current = null, auto = null)
        rule.onNodeWithContentDescription("Calendar").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Files").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(listOf<String?>("Calendar", "Files"), picks) }
    }

    @Test fun ownIconAndAutomaticAreChoicesToo() {
        picker(current = "Calendar", auto = SketchKind.Browser)
        rule.onNodeWithContentDescription("Own icon").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Automatic, now Browser").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(listOf(SketchChoices.OWN, null), picks) }
    }

    @Test fun everyDrawingIsOffered() {
        picker(current = null, auto = null)
        for (k in SketchKind.entries) rule.onNodeWithContentDescription(k.label).performScrollTo()
        rule.onNodeWithText("Cancel").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, cancelled); assertEquals(emptyList<String?>(), picks) }
    }
}
