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
 * Numio paper palette: cream paper, near-black ink, antique gold.
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
)

private val PaperLight = NumioColors(
    background = Color(0xFFFBF3E2),
    ink = Color(0xFF1C1A17),
    inkSoft = Color(0xFF6B6255),
    gold = Color(0xFFB8892B),
    card = Color(0xFFF3E6CA),
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

fun paperColors(dark: Boolean) = if (dark) PaperDark else PaperLight

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
