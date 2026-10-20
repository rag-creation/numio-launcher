package com.rr.numio.launcher.data

/**
 * Ink can hide the Android status bar for an uninterrupted sketchbook page. Only the
 * status bar: the navigation bar and gesture handle are never touched. Sol always keeps
 * it, and so do the Workspace and Selected-apps screens. A swipe down from the top edge
 * still shows it for a moment; turning the setting off brings it back for good.
 */
object StatusBarRule {

    fun hide(theme: HomeTheme, showStatusBar: Boolean, workspaceOpen: Boolean, selectedOpen: Boolean): Boolean =
        theme == HomeTheme.Ink && !showStatusBar && !workspaceOpen && !selectedOpen
}
