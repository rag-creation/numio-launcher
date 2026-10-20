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
import com.rr.numio.launcher.sketch.SketchPalette

/**
 * Numio paper palette. Ink is a sketchbook page in Light Ink (the website's ink #191815
 * and amber #E9A800 on paper, #7A5600 for amber text) or Dark Ink (#F1EEE6 ink and the
 * logo's #FFC21A on #0F0F0E). One design, two palettes.
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
    /** Amber for text (headings, letters): readable on the page, unlike the accent [gold] on light paper. */
    val goldText: Color = gold,
    /** The sketchbook page: paper grain, the landscape and the drawer's curled corner. */
    val sketchbook: Boolean = false,
    /** The dark sketchbook page (Dark Ink). */
    val pageDark: Boolean = false,
    /** Text on amber surfaces. */
    val onGold: Color = NumioBrand.ON_AMBER,
    /** The colours Ink's icons and drawings use on this page. */
    val icons: SketchPalette = SketchPalette.LIGHT_WARM,
)

/** Numio's official colours (website palette). The logo keeps its own #FFC21A untouched. */
object NumioBrand {
    val LOGO_AMBER = Color(0xFFFFC21A)
    val INK = Color(0xFF191815)
    val AMBER = Color(0xFFE9A800)
    val AMBER_TEXT = Color(0xFF7A5600)
    val ON_AMBER = Color(0xFF1A1400)
    val PAPER = Color(0xFFF6F4EE)
    val DARK_BG = Color(0xFF0F0F0E)
    val DARK_TEXT = Color(0xFFF1EEE6)

    /** Ink's own warmer sketchbook surface (Ink only; the website palette is unchanged). */
    val SKETCHBOOK = Color(0xFFEFE6D3)
}

/** Light Ink. [warm]: the warm sketchbook paper (default) or Numio's website paper. */
private fun inkLight(warm: Boolean) = NumioColors(
    background = if (warm) NumioBrand.SKETCHBOOK else NumioBrand.PAPER,
    ink = NumioBrand.INK,
    inkSoft = Color(0xFF5E5648),
    gold = NumioBrand.AMBER,
    card = if (warm) Color(0xFFF6F0E2) else Color(0xFFFBFAF6),
    goldText = NumioBrand.AMBER_TEXT,
    sketchbook = true,
    icons = if (warm) SketchPalette.LIGHT_WARM else SketchPalette.LIGHT_BRAND,
)

/** Dark Ink: the same page in dark, with cream ink and the logo's amber. */
private val InkDark = NumioColors(
    background = NumioBrand.DARK_BG,
    ink = NumioBrand.DARK_TEXT,
    inkSoft = Color(0xFFB5AFA2),
    gold = NumioBrand.LOGO_AMBER,
    card = Color(0xFF1C1B19),
    goldText = NumioBrand.LOGO_AMBER,
    sketchbook = true,
    pageDark = true,
    icons = SketchPalette.DARK,
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

/** Ink's page: Dark Ink when [dark], else Light Ink on warm or Numio paper. */
fun paperColors(dark: Boolean, warm: Boolean = true) = if (dark) InkDark else inkLight(warm)

fun homeColors(bg: Background, dark: Boolean, warm: Boolean = true) =
    if (bg == Background.Wallpaper) OnWallpaper else paperColors(dark, warm)

val LocalHome = staticCompositionLocalOf { inkLight(true) }
val LocalPaper = staticCompositionLocalOf { inkLight(true) }

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
