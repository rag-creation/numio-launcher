package com.rr.numio.launcher.sketch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import com.rr.numio.launcher.R
import kotlin.random.Random

/**
 * The hand-drawn pieces shared by the clock, icons and borders:
 * RR's own digit and A–Z sheets (same as Clock by Numio's Sketch widget) and wobbly ink lines.
 */
object SketchInk {

    // Listed by id (not looked up by name) so resource shrinking keeps them.
    private val DIGITS = intArrayOf(
        R.drawable.sketch_digit_0, R.drawable.sketch_digit_1, R.drawable.sketch_digit_2,
        R.drawable.sketch_digit_3, R.drawable.sketch_digit_4, R.drawable.sketch_digit_5,
        R.drawable.sketch_digit_6, R.drawable.sketch_digit_7, R.drawable.sketch_digit_8,
        R.drawable.sketch_digit_9
    )
    private val LETTERS = intArrayOf(
        R.drawable.sketch_letter_a, R.drawable.sketch_letter_b, R.drawable.sketch_letter_c,
        R.drawable.sketch_letter_d, R.drawable.sketch_letter_e, R.drawable.sketch_letter_f,
        R.drawable.sketch_letter_g, R.drawable.sketch_letter_h, R.drawable.sketch_letter_i,
        R.drawable.sketch_letter_j, R.drawable.sketch_letter_k, R.drawable.sketch_letter_l,
        R.drawable.sketch_letter_m, R.drawable.sketch_letter_n, R.drawable.sketch_letter_o,
        R.drawable.sketch_letter_p, R.drawable.sketch_letter_q, R.drawable.sketch_letter_r,
        R.drawable.sketch_letter_s, R.drawable.sketch_letter_t, R.drawable.sketch_letter_u,
        R.drawable.sketch_letter_v, R.drawable.sketch_letter_w, R.drawable.sketch_letter_x,
        R.drawable.sketch_letter_y, R.drawable.sketch_letter_z
    )

    private val glyphs = HashMap<Int, Bitmap>()

    fun hasGlyph(c: Char): Boolean = c.uppercaseChar() in 'A'..'Z' || c in '0'..'9'

    /** White-with-alpha glyph bitmap for A–Z / 0–9, or null. Thread-safe and cached. */
    fun glyph(context: Context, c: Char): Bitmap? {
        val u = c.uppercaseChar()
        val id = when (u) {
            in '0'..'9' -> DIGITS[u - '0']
            in 'A'..'Z' -> LETTERS[u - 'A']
            else -> return null
        }
        return synchronized(glyphs) {
            glyphs.getOrPut(id) {
                BitmapFactory.decodeResource(context.resources, id, BitmapFactory.Options().apply { inScaled = false })
            }
        }
    }

    // ── text made of glyph images (spacing as in Clock by Numio) ─────────────
    private fun gap(h: Float) = h * 0.07f
    private fun spaceW(h: Float) = h * 0.38f
    private fun colonW(h: Float) = h * 0.30f

    fun measure(context: Context, text: String, h: Float): Float {
        var w = 0f
        text.forEachIndexed { i, c ->
            w += when (c) {
                ' ' -> spaceW(h)
                ':' -> colonW(h)
                else -> glyph(context, c)?.let { it.width * h / it.height } ?: 0f
            }
            if (i < text.lastIndex) w += gap(h)
        }
        return w
    }

    /**
     * Draws [text] with its bottom edge on [bottom]. The colon is two inked dots in
     * [colonColor]. With [shadowColor] set, a soft shadow goes under it (for wallpapers).
     */
    fun drawText(
        context: Context, canvas: Canvas, text: String,
        x: Float, bottom: Float, h: Float, color: Int,
        colonColor: Int = color, shadowColor: Int? = null,
    ) {
        if (shadowColor != null) {
            val o = (h * 0.03f).coerceAtLeast(1.5f)
            drawText(context, canvas, text, x + o * 0.5f, bottom + o, h, shadowColor, shadowColor, null)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = colonColor }
        var cx = x
        text.forEachIndexed { i, c ->
            when (c) {
                ' ' -> cx += spaceW(h)
                ':' -> {
                    val r = h * 0.055f
                    val mx = cx + colonW(h) / 2f
                    canvas.drawCircle(mx, bottom - h * 0.66f, r, dot)
                    canvas.drawCircle(mx + r * 0.3f, bottom - h * 0.68f, r * 0.7f, dot)
                    canvas.drawCircle(mx - r * 0.1f, bottom - h * 0.24f, r * 1.05f, dot)
                    canvas.drawCircle(mx + r * 0.35f, bottom - h * 0.22f, r * 0.6f, dot)
                    cx += colonW(h)
                }
                else -> glyph(context, c)?.let { g ->
                    val gw = g.width * h / g.height
                    canvas.drawBitmap(g, null, RectF(cx, bottom - h, cx + gw, bottom), paint)
                    cx += gw
                }
            }
            if (i < text.lastIndex) cx += gap(h)
        }
    }

    /** A rounded rectangle traced by a slightly shaky hand. Same seed → same wobble. */
    fun wobblyRect(r: RectF, radius: Float, wobble: Float, seed: Int, steps: Int = 90): Path {
        val rnd = Random(seed)
        val src = Path().apply { addRoundRect(r, radius, radius, Path.Direction.CW) }
        val pm = PathMeasure(src, true)
        val len = pm.length
        val pos = FloatArray(2)
        val out = Path()
        var drift = 0f
        for (i in 0..steps) {
            pm.getPosTan(len * i / steps, pos, null)
            drift = (drift + (rnd.nextFloat() - 0.5f) * wobble * 0.6f).coerceIn(-wobble, wobble)
            val x = pos[0] + drift
            val y = pos[1] + drift * 0.6f
            if (i == 0) out.moveTo(x, y) else out.lineTo(x, y)
        }
        return out
    }

    /** A circle drawn by hand: the end overshoots the start a little, like a real pen. */
    fun wobblyCircle(cx: Float, cy: Float, radius: Float, wobble: Float, seed: Int): Path {
        val rnd = Random(seed)
        val steps = 64
        val start = rnd.nextFloat() * (2 * Math.PI).toFloat()
        val sweep = (2 * Math.PI).toFloat() * 1.06f
        val out = Path()
        var drift = 0f
        for (i in 0..steps) {
            val a = start + sweep * i / steps
            drift = (drift + (rnd.nextFloat() - 0.5f) * wobble * 0.7f).coerceIn(-wobble, wobble)
            val rr = radius + drift
            val x = cx + rr * kotlin.math.cos(a)
            val y = cy + rr * kotlin.math.sin(a)
            if (i == 0) out.moveTo(x, y) else out.lineTo(x, y)
        }
        return out
    }
}
