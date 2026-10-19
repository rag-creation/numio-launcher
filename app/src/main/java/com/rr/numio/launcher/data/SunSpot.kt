package com.rr.numio.launcher.data

import kotlin.math.max

/**
 * Where each Sol wallpaper's sun is, so the Numio logo can sit right inside it.
 * The suns were drawn at known spots on the 1080×2400 artwork; the wallpaper is shown
 * filling the screen (crop) with a vertical bias of [BIAS_Y], so the same maths finds the
 * sun on any screen. Pure Kotlin, unit tested.
 */
object SunSpot {
    const val IMG_W = 1080f
    const val IMG_H = 2400f
    /** Must match the wallpaper's BiasAlignment in SolScreen. */
    const val BIAS_Y = 0.4f

    /** The sun's centre on the artwork, in artwork pixels. */
    fun onArtwork(w: SolWallpaper): Pair<Float, Float> = when (w) {
        SolWallpaper.Petal -> 837f to 972f    // 0.775 × 1080, 0.405 × 2400
        SolWallpaper.Garden -> 1000f to 880f  // the dabbed sun, half off the right edge
    }

    /** The sun's centre on a screen (or box) of this size, in its pixels. */
    fun onScreen(w: SolWallpaper, screenW: Float, screenH: Float): Pair<Float, Float> {
        val s = max(screenW / IMG_W, screenH / IMG_H)
        val shownW = IMG_W * s
        val shownH = IMG_H * s
        val left = (screenW - shownW) / 2f
        val top = (screenH - shownH) / 2f * (1f + BIAS_Y)
        val (x, y) = onArtwork(w)
        return (left + x * s) to (top + y * s)
    }
}
