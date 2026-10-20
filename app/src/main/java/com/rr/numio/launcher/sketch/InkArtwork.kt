package com.rr.numio.launcher.sketch

import com.rr.numio.launcher.R

/**
 * Ink's two drawings, one per page. Light Ink keeps the original sketchbook landscape
 * (drawn from res/raw/ink_landscape.txt, unchanged). Dark Ink has its own night scene:
 * cream pen on black with a gold moon, a separate asset rather than a recoloured copy.
 * The page's palette already follows the Ink appearance setting (Light, Dark or Follow
 * system), never Samsung's dark mode on its own, so choosing by palette does the same.
 */
enum class InkArtwork {
    Sketchbook,
    Night;

    companion object {
        fun of(palette: SketchPalette): InkArtwork = if (palette.dark) Night else Sketchbook

        /** The night scene: the approved drawing's scenery band, its black made transparent. */
        val NIGHT_RES = R.drawable.ink_night
    }
}
