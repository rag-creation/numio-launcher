package com.rr.numio.launcher.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class StatusBarTest {

    @Test fun inkHidesItOnlyWhenAskedAndNothingElseIsOpen() {
        assertTrue(StatusBarRule.hide(HomeTheme.Ink, showStatusBar = false, workspaceOpen = false, selectedOpen = false))
        assertFalse(StatusBarRule.hide(HomeTheme.Ink, showStatusBar = true, workspaceOpen = false, selectedOpen = false))
        // the Workspace and Selected apps keep it
        assertFalse(StatusBarRule.hide(HomeTheme.Ink, showStatusBar = false, workspaceOpen = true, selectedOpen = false))
        assertFalse(StatusBarRule.hide(HomeTheme.Ink, showStatusBar = false, workspaceOpen = false, selectedOpen = true))
    }

    @Test fun solNeverHidesIt() {
        for (ws in listOf(false, true)) for (sel in listOf(false, true)) {
            assertFalse(StatusBarRule.hide(HomeTheme.Sol, showStatusBar = false, workspaceOpen = ws, selectedOpen = sel))
        }
    }

    @Test fun shownByDefaultAndTheChoiceIsSaved() = runBlocking {
        val prefs = HomePrefs(ApplicationProvider.getApplicationContext())
        assertTrue(prefs.inkStatusBar.first())
        prefs.setInkStatusBar(false)
        assertFalse(HomePrefs(ApplicationProvider.getApplicationContext()).inkStatusBar.first())
        prefs.setInkStatusBar(true)
        assertTrue(prefs.inkStatusBar.first())
    }

    @Test fun onlyTheStatusBarIsEverHidden() {
        // the launcher hides status bars only: never navigation bars or all system bars
        val src = File("src/main/java/com/rr/numio/launcher/MainActivity.kt").readText()
        assertTrue(src.contains("hide(WindowInsetsCompat.Type.statusBars())"))
        assertFalse(src.contains("Type.navigationBars())") && src.contains("hide(WindowInsetsCompat.Type.navigationBars"))
        assertFalse(src.contains("hide(WindowInsetsCompat.Type.systemBars"))
    }
}
