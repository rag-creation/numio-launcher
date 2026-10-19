package com.rr.numio.launcher.sketch

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/**
 * The hand-drawn treatment for an app's own icon artwork (its monochrome layer, read from
 * the phone at runtime and never copied into Numio): its edges wobble gently like a pen
 * line, a lighter second pass thickens one side, and the whole mark sits at a slight
 * hand tilt. Kept subtle so the app's silhouette and details stay recognisable.
 * Every app's own artwork in Ink gets it; Sol never does.
 */
object SketchTreatment {

    /** How far edges drift, as a share of the icon (about 1 px on a 52 dp icon). */
    const val WOBBLE = 0.008

    /** The tilt for [seed]: -1, -0.5, 0, 0.5 or 1 degree, fixed per app. */
    fun tilt(seed: Int): Float = (Math.floorMod(seed, 5) - 2) * 0.5f

    /** Edge wobble at [x], [y]: a smooth field, about 1.2% of the icon, same per [seed]. */
    private fun field(x: Float, y: Float, px: Int, seed: Int, axis: Int): Float {
        val s = (seed + axis * 977) * 0.618
        val u = x / px * 2 * PI
        val v = y / px * 2 * PI
        val w = 0.6 * sin(u * 1.7 + v * 0.9 + s) + 0.4 * sin(u * 4.3 - v * 3.1 + s * 1.9)
        return (w * px * WOBBLE).toFloat()
    }

    /** Bilinear alpha from [a] (px*px) at a fractional spot; 0 outside. */
    private fun alphaAt(a: IntArray, px: Int, x: Float, y: Float): Float {
        val x0 = floor(x).toInt()
        val y0 = floor(y).toInt()
        if (x0 < 0 || y0 < 0 || x0 >= px - 1 || y0 >= px - 1) return 0f
        val fx = x - x0
        val fy = y - y0
        val i = y0 * px + x0
        val top = a[i] * (1 - fx) + a[i + 1] * fx
        val bottom = a[i + px] * (1 - fx) + a[i + px + 1] * fx
        return top * (1 - fy) + bottom * fy
    }

    /** [layer] (one colour with alpha) redrawn by hand. Returns a new bitmap; [layer] is kept. */
    fun apply(layer: Bitmap, color: Int, seed: Int): Bitmap {
        val px = layer.width
        val src = IntArray(px * px)
        layer.getPixels(src, 0, px, 0, 0, px, px)
        val alpha = IntArray(px * px) { src[it] ushr 24 }

        // 1. the wobbled line
        val wobbled = IntArray(px * px)
        val rgb = color and 0xFFFFFF
        for (y in 0 until px) for (x in 0 until px) {
            val fx = x + field(x.toFloat(), y.toFloat(), px, seed, 0)
            val fy = y + field(x.toFloat(), y.toFloat(), px, seed, 1)
            val a = alphaAt(alpha, px, fx, fy).toInt().coerceIn(0, 255)
            wobbled[y * px + x] = (a shl 24) or rgb
        }
        val line = Bitmap.createBitmap(wobbled, px, px, Bitmap.Config.ARGB_8888)

        // 2. the second, lighter pass, a hair down-right, and 3. a slight tilt for the whole mark
        val out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.rotate(tilt(seed), px / 2f, px / 2f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.alpha = 110
        c.drawBitmap(line, px * 0.005f, px * 0.004f, paint)
        paint.alpha = 255
        c.drawBitmap(line, 0f, 0f, paint)
        line.recycle()
        return out
    }
}
