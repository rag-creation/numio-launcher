package com.rr.numio.launcher.sketch

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import androidx.core.graphics.PathParser
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Draws a Numio Sketch glyph with one consistent pen (the same maths as
 * art/sketch-icons/glyphs.py, which makes the preview sheets):
 *  - lines follow the path with a slow, gentle wobble, the same for every icon;
 *  - the line swells and thins along its length like a brush pen (0.8x to 1.15x);
 *  - fills sit a little off the outline; hatching is lighter than the outline.
 */
object SketchPen {

    /** The 56-unit content box spans this many disc radii. */
    const val SCALE = 1.55f
    const val INK_W = 4.4f
    const val THIN_W = 3.0f
    const val HATCH_W = 2.4f
    private const val STEP = 0.8f        // resample step, in grid units
    private const val WOBBLE = 0.55f     // grid units
    private const val FILL_OFFSET = 1.2f // grid units, down-right

    private val parsed = HashMap<String, Path>()

    private fun path(d: String): Path = synchronized(parsed) {
        parsed.getOrPut(d) { PathParser.createPathFromPathData(d) }
    }

    /** Seed for layer [index] of [kind]: fixed, so an icon looks the same everywhere. */
    fun seed(kind: SketchKind, index: Int): Int = kind.name.lowercase().sumOf { it.code } * 13 + index * 31

    /** Pen pressure along the line: a slow swell between 0.8x and 1.15x. */
    fun pressure(d: Float, seed: Int): Float {
        val p = (seed * 2.399) % (2 * PI)
        return (0.975 + 0.175 * sin(d * 2 * PI / 29 + p)).toFloat()
    }

    /** Sideways drift of the line at distance [d] (grid units). */
    fun drift(d: Float, seed: Int): Float {
        val p1 = (seed * 0.731) % (2 * PI)
        val p2 = (seed * 1.917) % (2 * PI)
        return (WOBBLE * (0.65 * sin(d * 2 * PI / 41 + p1) + 0.35 * sin(d * 2 * PI / 13 + p2))).toFloat()
    }

    /** Draws [kind] centred on (cx, cy) inside a disc of radius [r]. */
    fun draw(canvas: Canvas, kind: SketchKind, cx: Float, cy: Float, r: Float) {
        val layers = SketchGlyphs.all[kind] ?: return
        val sc = SCALE * r / 56f
        val toScreen = Matrix().apply {
            setTranslate(-50f, -50f)
            postScale(sc, sc)
            postTranslate(cx, cy)
        }
        layers.forEachIndexed { i, layer ->
            val seed = seed(kind, i)
            when (layer) {
                is Layer.Fill -> {
                    val p = Path(path(layer.path))
                    p.transform(toScreen)
                    p.offset(FILL_OFFSET * sc, FILL_OFFSET * sc)
                    canvas.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = layer.tone.argb })
                }
                is Layer.Ink -> line(canvas, path(layer.path), toScreen, sc, INK_W, Tone.INK.argb, 255, seed)
                is Layer.Thin -> line(canvas, path(layer.path), toScreen, sc, THIN_W, layer.tone.argb, 255, seed)
                is Layer.Hatch -> line(canvas, path(layer.path), toScreen, sc, HATCH_W, Tone.INK.argb, 200, seed)
                is Layer.Dot -> {
                    val pt = floatArrayOf(layer.x, layer.y)
                    toScreen.mapPoints(pt)
                    canvas.drawCircle(pt[0], pt[1], layer.r * sc, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = layer.tone.argb })
                }
            }
        }
    }

    /** Every contour of [src] as a pen line: resampled, drifted sideways, varying in width. */
    private fun line(canvas: Canvas, src: Path, toScreen: Matrix, sc: Float, width: Float, color: Int, alpha: Int, seed: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.alpha = alpha
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; this.alpha = alpha }
        val pm = PathMeasure(src, false)
        val pos = FloatArray(2)
        val tan = FloatArray(2)
        var contour = 0
        do {
            val len = pm.length
            if (len > 0f) {
                val s = seed + contour
                val n = maxOf(2, (len / STEP).toInt())
                val pts = FloatArray((n + 1) * 2)
                for (k in 0..n) {
                    val d = len * k / n
                    pm.getPosTan(d, pos, tan)
                    val w = drift(d, s)
                    pts[k * 2] = pos[0] - tan[1] * w
                    pts[k * 2 + 1] = pos[1] + tan[0] * w
                }
                toScreen.mapPoints(pts)
                var d = 0f
                for (k in 0 until n) {
                    val x0 = pts[k * 2]; val y0 = pts[k * 2 + 1]
                    val x1 = pts[k * 2 + 2]; val y1 = pts[k * 2 + 3]
                    paint.strokeWidth = width * pressure(d, s) * sc
                    canvas.drawLine(x0, y0, x1, y1, paint)
                    d += hypot(x1 - x0, y1 - y0) / sc
                }
                canvas.drawCircle(pts[0], pts[1], width * pressure(0f, s) * sc / 2f, dot)
            }
            contour++
        } while (pm.nextContour())
    }
}
