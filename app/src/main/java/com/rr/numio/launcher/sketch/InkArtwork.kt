package com.rr.numio.launcher.sketch

import com.rr.numio.launcher.R
import com.rr.numio.launcher.data.InkDrawing

/**
 * What Ink's page draws. Sketchbook: the original landscape (drawn from
 * res/raw/ink_landscape.txt, unchanged). Night lake: the approved pen drawing, as its own
 * asset for each page: cream pen on black for Dark Ink ([Night]), dark pen on paper for
 * Light Ink ([NightOnPaper]). Both keep the gold moon; neither is recoloured at run time.
 * The palette already follows the Ink appearance setting (Light, Dark or Follow system),
 * never Samsung's dark mode on its own.
 */
enum class InkArtwork(val res: Int) {
    Sketchbook(0),
    Night(R.drawable.ink_night),
    NightOnPaper(R.drawable.ink_night_paper);

    companion object {
        fun of(palette: SketchPalette, drawing: InkDrawing = InkDrawing.DEFAULT): InkArtwork = when (drawing) {
            // the default: each mode keeps its own drawing
            InkDrawing.Auto -> if (palette.dark) Night else Sketchbook
            InkDrawing.Sketchbook -> Sketchbook
            InkDrawing.NightLake -> if (palette.dark) Night else NightOnPaper
        }
    }
}
