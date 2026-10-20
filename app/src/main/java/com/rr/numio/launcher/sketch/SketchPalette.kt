package com.rr.numio.launcher.sketch

/**
 * The colours Ink's drawings use: icons, the landscape, the paper grain and the page curl.
 * One palette per page, so a theme switch changes every drawing together.
 */
data class SketchPalette(
    /** Part of icon cache keys, so each palette keeps its own icons. */
    val id: String,
    val ink: Int,
    val paper: Int,
    /** Fills inside drawn symbols (a little lighter than the page, or a little lighter dark). */
    val cream: Int,
    val gold: Int,
    val dark: Boolean,
) {
    companion object {
        /** Light Ink on the warm sketchbook paper (the default). */
        val LIGHT_WARM = SketchPalette("lw", 0xFF191815.toInt(), 0xFFEFE6D3.toInt(), 0xFFFFF9EE.toInt(), 0xFFE9A800.toInt(), false)

        /** Light Ink on Numio's website paper. */
        val LIGHT_BRAND = SketchPalette("lb", 0xFF191815.toInt(), 0xFFF6F4EE.toInt(), 0xFFFFFDF8.toInt(), 0xFFE9A800.toInt(), false)

        /** Dark Ink: cream ink and the logo's amber on the dark page. */
        val DARK = SketchPalette("d", 0xFFF1EEE6.toInt(), 0xFF0F0F0E.toInt(), 0xFF1E1D1A.toInt(), 0xFFFFC21A.toInt(), true)
    }
}
