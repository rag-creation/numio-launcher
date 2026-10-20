package com.rr.numio.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.rr.numio.launcher.data.Background

/**
 * Numio paper palette. Ink is a sketchbook page: warm paper (an Ink-only surface), the
 * website's ink #191815 and amber #E9A800, with #7A5600 for amber text on paper.
 * [home] is for the Canvas (paper or the wallpaper), [paper] for the Shelf and menus.
 */
@Immutable
data class NumioColors(
    val background: Color,
    val ink: Color,
    val inkSoft: Color,
    val gold: Color,
    val card: Color,
    /** True when drawn straight on the phone wallpaper (white ink with a shadow). */
    val onWallpaper: Boolean = false,
    /** Ink's hand-drawn look (sketch icons, letters, wobbly lines). False for Sol's clean look. */
    val sketch: Boolean = true,
    /** Amber for text (headings, letters): readable on paper, unlike the accent [gold]. */
    val goldText: Color = gold,
    /** The sketchbook page: paper grain, the landscape and the drawer's curled corner. */
    val sketchbook: Boolean = false,
)

/** Numio's official colours (website palette). The logo keeps its own #FFC21A untouched. */
object NumioBrand {
    val LOGO_AMBER = Color(0xFFFFC21A)
    val INK = Color(0xFF191815)
    val AMBER = Color(0xFFE9A800)
    val AMBER_TEXT = Color(0xFF7A5600)
    val PAPER = Color(0xFFF6F4EE)

    /** Ink's own warmer sketchbook surface (Ink only; the website palette is unchanged). */
    val SKETCHBOOK = Color(0xFFEFE6D3)
}

/** Ink: the sketchbook page. Always light, like a page of paper. */
private val PaperLight = NumioColors(
    background = NumioBrand.SKETCHBOOK,
    ink = NumioBrand.INK,
    inkSoft = Color(0xFF5E5648),
    gold = NumioBrand.AMBER,
    card = Color(0xFFF6F0E2),
    goldText = NumioBrand.AMBER_TEXT,
    sketchbook = true,
)

private val PaperDark = NumioColors(
    background = Color(0xFF1A1815),
    ink = Color(0xFFF3EAD7),
    inkSoft = Color(0xFFA79C88),
    gold = Color(0xFFD9A93A),
    card = Color(0xFF26231E),
)

private val OnWallpaper = NumioColors(
    background = Color.Transparent,
    ink = Color.White,
    inkSoft = Color(0xDDFFFFFF),
    gold = Color(0xFFF2C14E),
    card = Color(0x33000000),
    onWallpaper = true,
)

/** Numio Sol: warm cream paper, charcoal ink, mustard sun. Always light, to sit on its wallpaper. */
val SolColors = NumioColors(
    background = Color(0xFFF3EBDD),
    ink = Color(0xFF2B2722),
    inkSoft = Color(0xFF6E6458),
    gold = Color(0xFFD4A23A),
    card = Color(0xFFF7F1E6),
    sketch = false,
)

/** Ink's paper. The sketchbook page is always light; the older dark paper is kept for reference. */
@Suppress("UNUSED_PARAMETER")
fun paperColors(dark: Boolean) = PaperLight

/** Kept so the earlier dark Ink paper stays recoverable. */
internal val InkDarkPaper get() = PaperDark

fun homeColors(bg: Background, dark: Boolean) =
    if (bg == Background.Wallpaper) OnWallpaper else paperColors(dark)

val LocalHome = staticCompositionLocalOf { PaperLight }
val LocalPaper = staticCompositionLocalOf { PaperLight }

@Composable
fun NumioTheme(home: NumioColors, paper: NumioColors, dark: Boolean, content: @Composable () -> Unit) {
    // Material is only used for menus; they get the paper look
    val scheme = if (dark) {
        darkColorScheme(
            primary = paper.gold, surface = paper.card, onSurface = paper.ink,
            surfaceContainer = paper.card, background = paper.background, onBackground = paper.ink,
        )
    } else {
        lightColorScheme(
            primary = paper.gold, surface = paper.card, onSurface = paper.ink,
            surfaceContainer = paper.card, background = paper.background, onBackground = paper.ink,
        )
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalHome provides home, LocalPaper provides paper, content = content)
    }
}
